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

package org.spectral.asm.analyzer.frame

/**
 * An instruction frame which loads an index and array values from the stack.
 *
 * @property index Frame
 * @property array Frame
 * @constructor
 */
class ArrayLoadFrame(opcode: Int, val index: Frame, val array: Frame) : Frame(opcode) {

    /**
     * Update the modifications to the index and array
     * frames this frame accessed from the stack
     */
    init {
        this.index.children.add(this)
        this.array.children.add(this)
        this.parents.add(this.index)
        this.parents.add(this.array)
    }

}