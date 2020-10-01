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

/**
 * Represents a Java bytecode LINENUMBER instruction.
 *
 * @property line Int
 * @property start Label
 * @constructor
 */
class LineNumber(val line: Int, val start: Label) : Instruction(-1) {

    init {
        start.lineNumber = this
    }

    override fun accept(visitor: MethodVisitor) {
        visitor.visitLineNumber(line, start.label)
    }

    override fun toString(): String {
        return "LINENUMBER $line@L${start.id}"
    }
}