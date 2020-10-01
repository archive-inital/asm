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

package org.spectral.asm.analyzer.util

import org.objectweb.asm.Opcodes
import kotlin.reflect.KClass

/**
 * Primitive data type kotlin conversion utility methods.
 */
object PrimitiveUtils {

    private val nameToPrimitive = hashMapOf<String, KClass<*>>()
    private val defaultPrimitiveValues = hashMapOf<KClass<*>, Any?>()

    /**
     * Initialize the maps
     */
    init {
        /*
         * Default primitive values
         */
        defaultPrimitiveValues[Int::class] = 0
        defaultPrimitiveValues[Long::class] = 0L
        defaultPrimitiveValues[Double::class] = 0.0
        defaultPrimitiveValues[Float::class] = 0F
        defaultPrimitiveValues[Boolean::class] = false
        defaultPrimitiveValues[Char::class] = '\u0000'
        defaultPrimitiveValues[Short::class] = 0.toShort()
        defaultPrimitiveValues[Byte::class] = 0.toByte()
        defaultPrimitiveValues[Any::class] = null

        /*
         * Primitive type names
         */
        nameToPrimitive["int"] = Int::class
        nameToPrimitive["long"] = Long::class
        nameToPrimitive["double"] = Double::class
        nameToPrimitive["float"] = Float::class
        nameToPrimitive["boolean"] = Boolean::class
        nameToPrimitive["char"] = Char::class
        nameToPrimitive["byte"] = Byte::class
        nameToPrimitive["short"] = Short::class
        nameToPrimitive["void"] = Void::class
    }

    fun forName(name: String): KClass<*> {
        return nameToPrimitive[name] ?: throw NoSuchElementException("No primitive type with name '$name' found.")
    }

    fun defaultValueFor(type: KClass<*>): Any? {
        return defaultPrimitiveValues[type] ?: throw NoSuchElementException("No default primitive value for type '${type.simpleName}'.")
    }

    fun forArrayId(id: Int): KClass<*> {
        return when(id) {
            Opcodes.T_BOOLEAN -> Boolean::class
            Opcodes.T_CHAR -> Char::class
            Opcodes.T_FLOAT -> Float::class
            Opcodes.T_DOUBLE -> Double::class
            Opcodes.T_BYTE -> Byte::class
            Opcodes.T_SHORT -> Short::class
            Opcodes.T_INT -> Int::class
            Opcodes.T_LONG -> Long::class
            else -> throw IllegalArgumentException("Unknown type id: '$id'.")
        }
    }
}