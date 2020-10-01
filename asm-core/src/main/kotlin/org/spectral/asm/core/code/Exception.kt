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
import org.spectral.asm.core.reference.ClassRef

/**
 * Represents a try-catch exception block within the code.
 *
 * @property code Code
 * @property start Label
 * @property end Label
 * @property handler Label?
 * @property catchType ClassRef
 * @constructor
 */
class Exception(val code: Code, val start: Label, val end: Label, val handler: Label?, val catchType: ClassRef?) {

    fun accept(visitor: MethodVisitor) {
        visitor.visitTryCatchBlock(start.label, end.label, handler?.label, catchType?.name)
    }

}