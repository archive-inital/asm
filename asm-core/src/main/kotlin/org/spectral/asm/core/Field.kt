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

package org.spectral.asm.core

import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import java.lang.reflect.Modifier

/**
 * Represents a Java field which belongs to a class.
 *
 * @property owner Class
 * @constructor
 */
class Field(val owner: Class) : FieldVisitor(ASM9) {

    /**
     * The bit-packed visibility flags of the field.
     */
    var access = 0

    /**
     * The name of the field.
     */
    lateinit var name: String

    /**
     * The ASM [Type] of this field.
     */
    lateinit var type: Type

    /**
     * The descriptor of the field.
     */
    val desc: String get() = type.descriptor

    /**
     * The initialized value of the field.
     */
    var value: Any? = null

    /**
     * Creates a field with initialized visitor values.
     *
     * @param owner Class
     * @param access Int
     * @param name String
     * @param desc String
     * @param value Any?
     * @constructor
     */
    constructor(
            owner: Class,
            access: Int,
            name: String,
            desc: String,
            value: Any?
    ) : this(owner) {
        this.access = access
        this.name = name
        this.type = Type.getType(desc)
        this.value = value
    }

    /**
     * Whether the field is a static field.
     */
    val isStatic: Boolean get() = Modifier.isStatic(access)

    /**
     * Whether the field is a private field.
     */
    val isPrivate: Boolean get() = Modifier.isPrivate(access)

    /**
     * Initializes the field references.
     */
    internal fun init() {

    }

    override fun toString(): String {
        return "$owner.$name"
    }
}