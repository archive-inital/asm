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

package org.spectral.asm.analyzer.method

import org.objectweb.asm.tree.MethodNode
import org.spectral.asm.core.isAbstract
import org.spectral.asm.core.isNative

/**
 * Analyzes the method execution to create a data-flow graph.
 *
 * This allows to identify the type of data being pushed or popped from the stack
 * at any given instruction.
 */
object MethodAnalyzer {

    /**
     * Runs the analysis on the the provided [method].
     *
     * @param method MethodNode
     * @return AnalyzerResult
     */
    fun analyze(method: MethodNode): AnalyzerResult {
        /*
         * Abstract and native methods are not possible to analyze.
         */
        if(method.isAbstract || method.isNative) {
            return AnalyzerResult.EMPTY_RESULT
        }

        /*
         * The analysis result instance.
         */
        val result = AnalyzerResult()



        return result
    }
}