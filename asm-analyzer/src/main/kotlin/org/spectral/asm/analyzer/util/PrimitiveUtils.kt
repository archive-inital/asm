/*
 * Spectral Powered
 * Copyright (C) 2020 Kyle Escobar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, @see https://www.gnu.org/licenses/.
 */

package org.spectral.asm.analyzer.util

import org.objectweb.asm.Opcodes.*
import kotlin.reflect.KClass

/**
 * Utility methods for dealing with primitive wrapping.
 */
object PrimitiveUtils {

    /**
     * Primitive def information stores.
     */
    private val nameToPrimitive = hashMapOf<String, KClass<*>>()
    private val defaultPrimitiveValues = hashMapOf<KClass<*>, Any?>()

    /**
     * Build the primitive information
     */
    init {
        defaultPrimitiveValues[Int::class] = 0
        defaultPrimitiveValues[Long::class] = 0L
        defaultPrimitiveValues[Double::class] = 0.0
        defaultPrimitiveValues[Float::class] = 0F
        defaultPrimitiveValues[Boolean::class] = false
        defaultPrimitiveValues[Char::class] = '\u0000'
        defaultPrimitiveValues[Byte::class] = 0.toByte()
        defaultPrimitiveValues[Short::class] = 0.toShort()
        defaultPrimitiveValues[Any::class] = null
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

    /**
     * Finds the associated kotlin class of a primitive based on it's internal class name.
     *
     * @param name String
     * @return KClass<*>?
     */
    fun findPrimitive(name: String): KClass<*>? = nameToPrimitive[name]

    /**
     * Gets the default value of a primitive kotlin class.
     */
    val KClass<*>.defaultValue: Any?
        get() = defaultPrimitiveValues[this]

    /**
     * Gets the primitive kotlin class for an array id.
     *
     * @param id Int
     * @return KClass<*>
     */
    fun forArrayId(id: Int): KClass<*> {
        return when(id) {
            T_BOOLEAN -> Boolean::class
            T_CHAR -> Char::class
            T_FLOAT -> Float::class
            T_DOUBLE -> Double::class
            T_BYTE -> Byte::class
            T_SHORT -> Short::class
            T_INT -> Int::class
            T_LONG -> Long::class
            else -> throw IllegalArgumentException("Unknown type for array id: $id")
        }
    }
}