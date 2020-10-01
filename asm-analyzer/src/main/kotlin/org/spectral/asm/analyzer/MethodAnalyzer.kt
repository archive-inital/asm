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

import org.objectweb.asm.Opcodes
import org.spectral.asm.analyzer.frame.ArgumentFrame
import org.spectral.asm.core.Method

/**
 * Analyzes a Java method and outputs a [Frame] type for each instruction and what value type
 * it pushes to to the stack. It also tracks what other frames modify each other frames.
 */
object MethodAnalyzer {

    /**
     * Analyze a given method's execution frames and returns all the frames
     * in an [AnalyzerResult] object.
     *
     * @param method Method
     * @return AnalyzerResult
     */
    fun analyze(method: Method): AnalyzerResult {
        /*
         * Abstract and native method types do not have any
         * execution logic. Therefore, we cannot analyze their execution.
         */
        if(method.isAbstract || method.isNative) {
            return AnalyzerResult.EMPTY_RESULT
        }

        /**
         * The analysis result object.
         */
        val result = AnalyzerResult()

        /**
         * A list of execution stack states or (contexts) at each execution frame (each instruction).
         */
        val stack = mutableListOf<StackContext>()

        /**
         * A list of execution LVT states or (contexts) at each execution frame (each instruction).
         */
        val locals = mutableListOf<StackContext>()

        /**
         * The instruction index we are currently at.
         */
        var currentIndex = 0

        /*
         * If the method is NOT static, we need to add the 'this' local variable as an ASTORE instruction to store
         * it's reference to the LVT in the stack.
         */
        if(!method.isStatic) {
            locals.add(StackContext(ArgumentFrame(Opcodes.ASTORE, 0), method.owner.name))
            currentIndex++
        }

        /*
         * Load all the argument types from the method onto the LVT.
         */
        method.argumentTypes.forEach { type ->
        }

        throw Exception()
    }
}