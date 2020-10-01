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
 * An instruction frame which loads a value from the LVT at a specified index.
 *
 * @property local Int
 * @property value Frame?
 * @constructor
 */
class LocalFrame(opcode: Int, val local: Int, val value: Frame?) : Frame(opcode) {

    init {
        /*
         * Add this frame as a child modifier of the value frame. Only if the
         * provided frame value is not null.
         */
        if(this.value != null) {
            this.value.reads.add(this)
        }
    }

}