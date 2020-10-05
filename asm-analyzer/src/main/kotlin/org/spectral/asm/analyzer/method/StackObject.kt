/*
 * Spectral Powered
 * Copyright (C) 2020 Kyle Escobar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, @see https://www.gnu.org/licenses/.
 */

package org.spectral.asm.analyzer.method

import org.spectral.asm.analyzer.method.frame.ArgumentFrame
import org.spectral.asm.analyzer.method.frame.Frame
import org.spectral.asm.analyzer.method.value.ValueType
import kotlin.reflect.KClass

/**
 * Represents the JVM stack at a given instruction frame.
 *
 * @property type KClass<*>
 * @property value Frame
 * @property desc String?
 * @constructor
 */
class StackObject(val type: KClass<*>, val value: Frame?, desc: String?) {

    /**
     * Whether this object has been initialized with a value.
     */
    var isInitialized: Boolean = true

    /**
     * Whether this stack object represents the 'this' LVT entry.
     */
    var isThis: Boolean = false

    /**
     * The initialized type descriptor.
     */
    var initType: String? = desc

    /**
     * Gets the [ValueType] of the stack object.
     */
    val valueType: ValueType get() {
        return when {
            value == null -> ValueType.NULL
            (isThis && !isInitialized) -> ValueType.UNINITIALIZED_THIS
            (!isInitialized) -> ValueType.UNINITIALIZED
            type == Int::class -> ValueType.INT
            type == Double::class -> ValueType.DOUBLE
            type == Float::class -> ValueType.FLOAT
            type == Long::class -> ValueType.LONG
            type == Any::class -> ValueType.OBJECT
            type == Boolean::class -> ValueType.INT
            type == Short::class -> ValueType.INT
            type == Byte::class -> ValueType.INT
            type == Char::class -> ValueType.INT
            else -> throw IllegalArgumentException("Unable to determine value type of stack object.")
        }
    }

    /**
     * Creates a stack object without a initialized type descriptor.
     *
     * @param type KClass<*>
     * @param value Frame
     * @constructor
     */
    constructor(type: KClass<*>, value: Frame) : this(type, value, null)

    /**
     * Creates a stack object from an [ArgumentFrame] instance.
     *
     * @param frame ArgumentFrame
     * @param type String
     * @constructor
     */
    constructor(frame: ArgumentFrame, type: String) : this(Any::class, frame, type) {
        this.isInitialized = false
        this.isThis = true
    }

    /**
     * Initialize the stack context value.
     */
    fun initialize() {
        this.isInitialized = true
    }

    override fun toString(): String {
        return "[" + (value?.toString() ?: "null:$initType") + "]"
    }
}