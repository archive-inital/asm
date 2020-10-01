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
import org.objectweb.asm.util.Printer
import org.spectral.asm.core.code.Code

/**
 * Represents a Java bytecode instruction within a method.
 *
 * @property opcode Int
 * @constructor
 */
open class Instruction(val opcode: Int) {

    /**
     * Gets the instruction index value for the associated code list.
     */
    val insnIndex: Int get() = code.instructions.indexOf(this)

    /**
     * The previous instruction in the associated [code] instruction list.
     */
    val prev: Instruction? get() = code.instructions.getOrNull(insnIndex - 1)

    /**
     * The next instruction in the associated [code] instruction list.
     */
    val next: Instruction? get() = code.instructions.getOrNull(insnIndex + 1)

    /**
     * The [Code] object this instruction belongs in.
     */
    lateinit var code: Code internal set

    /**
     * Makes the given [visitor] visit this instruction.
     *
     * @param visitor MethodVisitor
     */
    open fun accept(visitor: MethodVisitor) {
        visitor.visitInsn(opcode)
    }

    /**
     * Initializes the references for this instruction.
     */
    internal open fun init() {
        /*
         * Nothing to do.
         */
    }

    override fun toString(): String {
        if(opcode == -1) {
            throw RuntimeException("Invalid opcode -1 does not have string representation override.")
        }

        return Printer.OPCODES[opcode]
    }
}