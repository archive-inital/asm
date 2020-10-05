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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Represents a collection of ASM [ClassNode] objects.
 */
class ClassPool {

    /**
     * The backing storage of the class pool.
     */
    private val classMap = LinkedHashMap<String, ClassNode>()

    /**
     * A list of the [ClassNode]s in this pool
     */
    val classes: List<ClassNode> get() = classMap.values.toList()

    /**
     * The number of elements inside of the class pool.
     */
    val size: Int get() = classMap.entries.size

    /**
     * Initializes the pool.
     */
    fun init() {
        classes.forEach { it.init() }
    }

    /**
     * Adds a [ClassNode] to the pool.
     *
     * @param element ClassNode
     */
    fun addClass(element: ClassNode) {
        if(this.classMap.containsKey(element.name)) {
            throw IllegalArgumentException("Class with name '${element.name}' already exists in the pool.")
        }

        element.pool = this
        classMap[element.name] = element
    }

    /**
     * Adds a [ClassNode] to the pool from the raw class bytes.
     *
     * @param bytes ByteArray
     */
    fun addClass(bytes: ByteArray) {
        val node = ClassNode()
        val reader = ClassReader(bytes)

        reader.accept(node, ClassReader.SKIP_FRAMES)

        return this.addClass(node)
    }

    /**
     * Removes a [ClassNode] from the pool.
     *
     * @param element ClassNode
     */
    fun removeClass(element: ClassNode) {
        if(!this.classMap.containsKey(element.name)) {
            throw NoSuchElementException("No class with name '${element.name}' found in the pool.")
        }

        element.pool = this
        classMap.remove(element.name)
    }

    /**
     * Gets a [ClassNode] by the class name if it exists in the pool.
     *
     * @param name String
     * @return ClassNode?
     */
    fun findClass(name: String): ClassNode? {
        return classMap[name]
    }

    /**
     * Exports the current pool's classes to a Jar file.
     *
     * @param file File
     */
    fun saveToJar(file: File) {
        if(file.exists()) {
            file.delete()
        }

        val jos = JarOutputStream(FileOutputStream(file))
        classMap.values.forEach { cls ->
            jos.putNextEntry(JarEntry(cls.name + ".class"))

            val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
            cls.accept(writer)

            jos.write(writer.toByteArray())
            jos.closeEntry()
        }

        jos.close()
    }

    companion object {

        /**
         * Creates a [ClassPool] with all classes loaded from a Jar file.
         *
         * @param file File
         * @return ClassPool
         */
        fun loadFromJar(file: File): ClassPool {
            val pool = ClassPool()

            val jar = JarFile(file)
            val entries = jar.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .iterator()

            while(entries.hasNext()) {
                val bytes = jar.getInputStream(entries.next()).readAllBytes()
                pool.addClass(bytes)
            }

            jar.close()

            /*
             * Initialize the pool
             */
            pool.init()

            return pool
        }
    }
}