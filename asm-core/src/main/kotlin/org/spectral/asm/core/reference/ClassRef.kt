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

package org.spectral.asm.core.reference

import org.spectral.asm.core.Class
import org.spectral.asm.core.ClassPool

/**
 * Represents a reference to a java class that may or may not
 * be loaded in a class pool.
 *
 * @property name String
 * @constructor
 */
class ClassRef(val name: String) {

    var ref: Class? = null

    /**
     * Init the [ref] object if contained in the pool.
     *
     * @param pool ClassPool
     */
    internal fun init(pool: ClassPool) {
        this.ref = pool[this.name]
    }

    override fun toString(): String {
        return name
    }
}