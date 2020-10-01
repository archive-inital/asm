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
import org.objectweb.asm.Label as AsmLabel

/**
 * Represents a bytecode label instruction.
 * @property label Label
 * @constructor
 */
class Label(val label: AsmLabel) : Instruction(-1) {

    /**
     * The [LineNumber] instruction associated with this label.
     */
    var lineNumber: LineNumber? = null
        internal set

    /**
     * The label index in the associated [code] labelMap.
     */
    var id: Int = -1
        internal set

    override fun accept(visitor: MethodVisitor) {
        visitor.visitLabel(label)
    }

    override fun toString(): String {
        return "LABEL $id"
    }
}