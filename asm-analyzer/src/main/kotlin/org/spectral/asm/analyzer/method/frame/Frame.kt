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

package org.spectral.asm.analyzer.method.frame

import org.objectweb.asm.util.Printer

/**
 * Represents an instruction execution step. NOT what the JVM refers to as a method frame.
 *
 * @property opcode Int
 * @constructor
 */
open class Frame(val opcode: Int) {

    /**
     * The mnemonic name of the instruction opcode.
     */
    open val mnemonic = if(opcode == -1) "UNKNOWN" else Printer.OPCODES[opcode]

    /**
     * A list of [Frame] instances this frame contributed to creating or is a value dependency of.
     */
    val parents = mutableListOf<Frame>()

    /**
     * A list of [Frame] instances which contributed to this frame's state.
     */
    val children = mutableListOf<Frame>()

    /**
     * Whether this frame holds a constant value.
     */
    open val isConstant: Boolean by lazy { calculateConstant() }

    /**
     * Calculates whether this frame holds a constant type.
     * All the parent frames are accounted for and the result is inherited.
     *
     * @return Boolean
     */
    private fun calculateConstant(): Boolean {
       var ret = true

        parents.forEach { frame ->
            ret = ret and frame.isConstant
        }

        return ret
    }

    override fun toString(): String {
        return "FRAME[$mnemonic]"
    }
}