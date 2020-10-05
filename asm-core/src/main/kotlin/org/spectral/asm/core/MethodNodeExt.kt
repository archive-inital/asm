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
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier

/**
 * The method class node owners storage.
 */
private val ownerValues = hashMapOf<MethodNode, ClassNode>()

/**
 * Initializes the methods.
 *
 * @receiver MethodNode
 */
internal fun MethodNode.init() {

}

/**
 * The owner [ClassNode] this method belongs in.
 */
var MethodNode.owner: ClassNode
    get() = ownerValues[this]!!
    set(value) {
        ownerValues[this] = value
    }

/**
 * The ASM [Type] of this method node.
 */
val MethodNode.type: Type get() = Type.getMethodType(this.desc)

/**
 * A list of ASM [Type]s for the arguments of this method.
 */
val MethodNode.argumentTypes: List<Type> get() = this.type.argumentTypes.toList()

/**
 * The ASM [Type] of this method's return value.
 */
val MethodNode.returnType: Type get() = this.type.returnType

/**
 * The [ClassPool] this method's owner belongs in.
 */
val MethodNode.pool: ClassPool get() = this.owner.pool

/**
 * Whether this method is static.
 */
val MethodNode.isStatic: Boolean get() = Modifier.isStatic(this.access)

/**
 * Whether this method is private.
 */
val MethodNode.isPrivate: Boolean get() = Modifier.isPrivate(this.access)

/**
 * Whether this method is an abstract method.
 */
val MethodNode.isAbstract: Boolean get() = Modifier.isAbstract(this.access)