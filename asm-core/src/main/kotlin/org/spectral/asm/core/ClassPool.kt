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

package org.spectral.asm.core

import org.objectweb.asm.ClassReader

/**
 * Represents a collection of [Class] objects loaded from a class path.
 *
 * @property classMap HashMap<String, Class>
 */
class ClassPool {

    /**
     * Private storage of [Class]s which are loaded from a common classpath.
     */
    private val classMap = hashMapOf<String, Class>()

    /**
     * A list of [Class] objects contained in the class map.
     */
    val classes: List<Class> get() = classMap.values.toList()

    /**
     * Adds a [Class] to this pool.
     *
     * @param element Class
     */
    fun addClass(element: Class) {
        if(classMap.containsKey(element.name)) {
            throw IllegalStateException("Class with name ${element.name} already exists in the pool")
        }

        element.pool = this
        classMap[element.name] = element
    }

    /**
     * Adds a [Class] from the raw Bytes to this pool.
     *
     * @param bytes ByteArray
     */
    fun addClass(bytes: ByteArray) {
        val reader = ClassReader(bytes)
        val cls = Class()

        reader.accept(cls, ClassReader.SKIP_FRAMES)

        this.addClass(cls)
    }

    /**
     * Removes a [Class] from this pool.
     *
     * @param element Class
     */
    fun removeClass(element: Class) {
        if(!classMap.containsKey(element.name)) {
            throw NoSuchElementException("No class with name ${element.name} found in the pool.")
        }

        classMap.remove(element.name)
    }

    /**
     * Gets a [Class] with a given class name.
     *
     * @param name String
     * @return Class?
     */
    operator fun get(name: String): Class? = classMap[name]
}