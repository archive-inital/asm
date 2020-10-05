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

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.lang.reflect.Modifier

/**
 * The owning [ClassNode] of the key [FieldNode].
 */
private val ownerValues = hashMapOf<FieldNode, ClassNode>()

/**
 * Initialize the field node.
 */
internal fun FieldNode.init() {

}

/**
 * The [ClassNode] this field belongs in.
 */
var FieldNode.owner: ClassNode
    get() = ownerValues[this]!!
    set(value) {
        ownerValues[this] = value
    }

/**
 * The [ClassPool] of this field's owner.
 */
val FieldNode.pool: ClassPool get() = this.owner.pool

/**
 * Whether this field is static.
 */
val FieldNode.isStatic: Boolean get() = Modifier.isStatic(this.access)

/**
 * Whether this field is private.
 */
val FieldNode.isPrivate: Boolean get() = Modifier.isPrivate(this.access)