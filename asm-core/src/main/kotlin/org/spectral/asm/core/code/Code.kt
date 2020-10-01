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

package org.spectral.asm.core.code

import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.Method
import java.util.*

/**
 * Represents the instructions contained within a java method.
 *
 * @property method Method
 * @constructor
 */
class Code(val method: Method) {

    /**
     * The backing storage of instruction objects contained within this object.
     */
    val instructions = LinkedList<Instruction>()

    /**
     * The maximum size of the stack for the associated [method].
     */
    var maxStack = 0

    /**
     * The maximum size of the LVT for the associated [method].
     */
    var maxLocals = 0

    /**
     * Makes the given [visitor] visit all the instruction related values in this object.
     *
     * @param visitor MethodVisitor
     */
    fun accept(visitor: MethodVisitor) {

    }

    /**
     * Initializes all the references of this object.
     */
    internal fun init() {
        /*
         * Set all the instructions [Code] reference to this object.
         */
        instructions.forEach { insn ->
            insn.code = this
            insn.init()
        }
    }
}