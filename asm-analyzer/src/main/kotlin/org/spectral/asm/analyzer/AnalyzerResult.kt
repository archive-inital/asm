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

import org.spectral.asm.analyzer.frame.Frame
import org.spectral.asm.core.code.Instruction

/**
 * Represents the returned results of a analysis of some type.
 */
class AnalyzerResult {

    /**
     * The max number of entries on the JVM stack.
     */
    var maxStack: Int = 0
        internal set

    /**
     * The max number of entries in the LVT.
     */
    var maxLocals: Int = 0
        internal set

    /**
     * The instruction frames in this analysis mapped to the instruction that was executed.
     */
    val frames = hashMapOf<Instruction, MutableList<Frame>>()

    companion object {
        /**
         * An empty analysis result object.
         */
        val EMPTY_RESULT = AnalyzerResult()
    }
}