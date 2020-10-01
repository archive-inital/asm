/*
 *     Spectral Powered
 *     Copyright (C) 2020 Kyle Escobar
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spectral.asm.analyzer

import com.google.common.primitives.Primitives
import org.spectral.asm.analyzer.frame.ArgumentFrame
import org.spectral.asm.analyzer.frame.Frame
import org.spectral.asm.analyzer.value.ValueType
import kotlin.reflect.KClass

/**
 * Represents a snapshot in time of the JVM stack during an execution step or
 * execution frame.
 *
 * @constructor
 */
open class StackContext {

    val type: KClass<*>

    val value: Frame?

    /**
     * The initialized type descriptor.
     */
    val initType: String?

    val isThis: Boolean

    var isInitialized: Boolean

    constructor(type: KClass<*>, value: Frame, desc: String?) {
        if(Primitives.unwrap(type.java) != type.java) {
            throw IllegalArgumentException()
        }

        this.type = type
        this.value = value
        this.initType = desc
        this.isThis = false
        this.isInitialized = true

        if(type == Any::class && desc == null) {
            throw IllegalArgumentException()
        }
    }

    /**
     * Create a stack context with a null descriptor.
     *
     * @param type KClass<*>
     * @param value Frame
     * @constructor
     */
    constructor(type: KClass<*>, value: Frame) : this(type, value, null)

    /**
     * Creates a stack context from an argument execution frame.
     *
     * @param frame ArgumentFrame
     * @param type String
     * @constructor
     */
    constructor(frame: ArgumentFrame, type: String) {
        this.isInitialized = false
        this.isThis = true
        this.type = Any::class
        this.value = frame
        this.initType = type
    }

    /**
     * Initializes the stack context.
     */
    fun initialize() {
        this.isInitialized = true
    }

    /**
     * Gets the [ValueType] of this contexts frame.
     */
    val valueType: ValueType get() {
        return when {
            value == null -> ValueType.NULL
            (isThis && !isInitialized) -> ValueType.UNINITIALIZED_THIS
            (!isInitialized) -> ValueType.UNINITIALIZED
            (type == Int::class) -> ValueType.INT
            (type == Double::class) -> ValueType.DOUBLE
            (type == Float::class) -> ValueType.FLOAT
            (type == Long::class) -> ValueType.LONG
            (type == Any::class) -> ValueType.OBJECT
            (type == Boolean::class) -> ValueType.INT
            (type == Short::class) -> ValueType.INT
            (type == Byte::class) -> ValueType.INT
            (type == Char::class) -> ValueType.INT
            else -> throw IllegalArgumentException("Unexpected value type $type $this")
        }
    }

    override fun toString(): String {
        return value?.toString() ?: "null type=$initType"
    }
}