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

package org.spectral.asm.analyzer

import org.spectral.asm.core.ClassPool
import org.spectral.asm.core.util.JarUtil
import java.io.File

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        val pool = ClassPool()
        val jarFile = File("Z:\\Data\\Libraries\\Projects\\Runescape\\Spectral\\asm\\gamepack.jar")

        JarUtil.readJar(jarFile).forEach { pool.addClass(it) }

        pool.init()

        val cls = pool["client"]!!
        val method = cls.findMethod("init", "()V")!!

        val analyzer = MethodAnalyzer.analyze(method)

    }
}