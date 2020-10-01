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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import org.spectral.asm.core.ClassPool
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Utility classes for loading class data from JAR files.
 */
object JarUtil {

    /**
     * Reads each .class entry in a Jar file and returns a set of [ByteArray] for each entry.
     *
     * @param file File
     * @return Set<ByteArray>
     */
    fun readJar(file: File): Set<ByteArray> {
        val bytes = mutableSetOf<ByteArray>()

        JarFile(file).use { jar ->
            jar.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .forEach {
                        bytes.add(jar.getInputStream(it).readAllBytes())
                    }
        }

        return bytes
    }

    /**
     * Writes all entries in a [ClassPool] to a Jar file.
     *
     * @param file File
     * @param pool ClassPool
     */
    fun writeJar(file: File, pool: ClassPool) {
        if(file.exists()) {
            file.delete()
        }

        val jos = JarOutputStream(FileOutputStream(file))

        pool.classes.forEach { cls ->
            val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
            val verifier = CheckClassAdapter(writer, false)

            cls.accept(verifier)

            val bytes = writer.toByteArray()

            /*
             * Validate data flow in class.
             */
            validateClassBytes( bytes)

            jos.putNextEntry(JarEntry(cls.name + ".class"))
            jos.write(bytes)
            jos.closeEntry()
        }

        jos.close()
    }

    /**
     * Validates that the [bytes] pass the JVM checks and will execute correctly.
     *
     * @param bytes ByteArray
     */
    private fun validateClassBytes(bytes: ByteArray) {
       try {
           val reader = ClassReader(bytes)
           val writer = ClassWriter(reader, 0)

           val checker = CheckClassAdapter(writer, true)
           reader.accept(checker, 0)
       } catch(e : Exception) {
           throw(e)
       }
    }
}