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

package org.spectral.asm.analyzer.method.value

import org.objectweb.asm.Type

/**
 * Represents the type of values pushed / popped from the JVM stack.
 */
enum class ValueType(val type: Type?) {
    TOP(null),
    INT(Type.INT_TYPE),
    FLOAT(Type.FLOAT_TYPE),
    LONG(Type.LONG_TYPE),
    DOUBLE(Type.DOUBLE_TYPE),
    NULL(null),
    UNINITIALIZED_THIS(null),
    OBJECT(null),
    UNINITIALIZED(null);
}