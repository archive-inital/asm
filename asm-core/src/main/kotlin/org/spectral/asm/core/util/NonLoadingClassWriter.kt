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

package org.spectral.asm.core.util

import org.objectweb.asm.ClassWriter
import org.spectral.asm.core.Class
import org.spectral.asm.core.ClassPool

/**
 * A class writer which respects hierarchy class names from a given [ClassPool].
 *
 * @property pool ClassPool
 * @property flags Int
 * @constructor
 */
class NonLoadingClassWriter(val pool: ClassPool, val flags: Int) : ClassWriter(flags) {

    /**
     * Gets the common super class name between [a] and [b] in the [pool] if any exists.
     *
     * @param a String
     * @param b String
     * @return String
     */
    override fun getCommonSuperClass(a: String, b: String): String {
        /*
         * Ignore 'java/lang/Object' as it does not have a super class.
         */
        if(a == "java/lang/Object" || b == "java/lang/Object") {
            return "java/lang/Object"
        }

        val classA = pool[a]
        val classB = pool[b]

        if(classA == null && classB == null) {
            return try {
                super.getCommonSuperClass(a, b)
            } catch (e: Exception) {
                /*
                 * Return default object class when a common super class is not found.
                 */
                "java/lang/Object"
            }
        }

        if(classA != null && classB != null) {
            if(!(classA.isInterface || classB.isInterface)) {
                var c1 = classA
                while(c1 != null) {
                    var c2 = classB
                    while(c2 != null) {
                        if(c1 == c2) return c1.name
                        c2 = c2.parent.ref
                    }
                    c1 = c1.parent.ref
                }
            }

            return "java/lang/Object"
        }

        val found: Class
        val other: String

        if(classA == null) {
            found = classB!!
            other = a
        } else {
            found = classA
            other = b
        }

        var prev: Class? = null

        var parent: Class? = found
        while(parent != null) {
            prev = parent
            if(prev.parent.name == other) return other
            parent = parent.parent.ref
        }

        return super.getCommonSuperClass(prev!!.parent.name, other)
    }
}