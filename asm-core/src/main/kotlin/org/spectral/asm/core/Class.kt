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

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.spectral.asm.core.reference.ClassRef

/**
 * Represents an Java class loaded from it's bytecode.
 */
class Class : ClassVisitor(ASM9) {

    /**
     * The [ClassPool] this class belongs in.
     */
    lateinit var pool: ClassPool internal set

    /**
     * The name of the class.
     */
    lateinit var name: String internal set

    /**
     * The source file name this class was loaded from.
     */
    lateinit var source: String

    /**
     * The access modifier flags bit-packed into an integer.
     */
    var access = 0

    /**
     * The JVM version flag this class was compiled in.
     */
    var version = 0

    /**
     * The parent super class reference.
     */
    lateinit var parent: ClassRef

    /**
     * The class names of interfaces this class implements.
     */
    val interfaces = mutableListOf<ClassRef>()

    /**
     * The classes which extend this class.
     */
    val children = mutableListOf<Class>()

    /**
     * The classes which implement this class.
     */
    val implementers = mutableListOf<Class>()

    /**
     * Initialize the class.
     */
    internal fun init() {
        /*
         * Build References.
         */
        this.parent.init(this.pool)
        this.interfaces.forEach { it.init(this.pool) }

        /*
         * Build hierarchy graph.
         */
        this.parent.ref?.children?.add(this)
        this.interfaces.mapNotNull { it.ref }.forEach { it.implementers.add(this) }
    }

    /*
     * VISITOR METHODS
     */

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String,
            interfaces: Array<String>
    ) {
        this.version = version
        this.access = access
        this.name = name
        this.parent = ClassRef(superName)
        this.interfaces.addAll(interfaces.map { ClassRef(it) })
    }

    override fun visitSource(source: String, debug: String?) {
        this.source = source
    }

    override fun visitEnd() {
        /*
         * Nothing to do.
         */
    }

    fun accept(visitor: ClassVisitor) {

    }

    override fun toString(): String {
        return name
    }
}