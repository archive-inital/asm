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

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import java.lang.reflect.Modifier

/**
 * Represents a Java method which is contained within a class.
 *
 * @property owner Class
 * @constructor
 */
class Method(val owner: Class) : MethodVisitor(ASM9) {

    /**
     * The bit-packed visibility acces flags of the method.
     */
    var access = 0

    /**
     * The name of the method.
     */
    lateinit var name: String

    /**
     * The descriptor of the method
     */
    lateinit var desc: String

    /**
     * Creates a new method with initialized visitor values.
     *
     * @param owner Class
     * @param access Int
     * @param name String
     * @param desc String
     * @constructor
     */
    constructor(owner: Class, access: Int, name: String, desc: String) : this(owner) {
        this.access = access
        this.name = name
        this.desc = desc
    }

    /**
     * Whether the method is a static method.
     */
    val isStatic: Boolean get() = Modifier.isStatic(access)

    /**
     * Whether the field is a private field.
     */
    val isPrivate: Boolean get() = Modifier.isPrivate(access)

    /**
     * Initializes the method refs.
     */
    internal fun init() {

    }

    override fun toString(): String {
        return "$owner.$name$desc"
    }
}