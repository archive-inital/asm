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

package org.spectral.asm.core

import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.lang.reflect.Modifier

/**
 * Storage of [ClassPool] instance references.
 */
private val poolValues = hashMapOf<ClassNode, ClassPool>()

/**
 * Initializes the class node and any child elements.
 *
 * @receiver ClassNode
 */
internal fun ClassNode.init() {
    /*
     * Initialize all methods in this class.
     */
    this.methods.forEach { method ->
        method.owner = this
        method.init()
    }

    /*
     * Initialize all fields in the class.
     */
    this.fields.forEach { field ->
        field.owner = this
        field.init()
    }
}

/**
 * The associated [ClassPool] this [ClassNode] belongs to.
 */
var ClassNode.pool: ClassPool
    get() = poolValues[this]!!
    set(value) {
        if(poolValues.containsKey(this)) {
            poolValues.remove(this)
        } else {
            poolValues[this] = value
        }
    }

/**
 * The ASM [Type] of this class node.
 */
val ClassNode.type: Type get() = Type.getObjectType(this.name)

/**
 * Whether the class node is an abstract class.
 */
val ClassNode.isAbstract: Boolean get() = Modifier.isAbstract(this.access)

/**
 * Whether the class node is an interface class.
 */
val ClassNode.isInterface: Boolean get() = Modifier.isInterface(this.access)

/**
 * Whether the class node is a native class.
 */
val ClassNode.isNative: Boolean get() = Modifier.isNative(this.access)