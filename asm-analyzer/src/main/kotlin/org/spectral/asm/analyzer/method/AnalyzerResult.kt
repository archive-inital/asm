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

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.tree.AbstractInsnNode
import org.spectral.asm.analyzer.method.frame.Frame

class AnalyzerResult {

    /**
     * The frames of this analysis.
     */
    val frames = MultimapBuilder.hashKeys().arrayListValues().build<AbstractInsnNode, Frame>()

    /**
     * The frame to instruction mapping.
     */
    var mappings: MutableMap<Frame, AbstractInsnNode>? = mutableMapOf<Frame, AbstractInsnNode>()

    /**
     * The reversed mapping for resolution speed.
     */
    private val revered = mutableMapOf<Frame, AbstractInsnNode>()

    /**
     * The maximum number of locals in this analysis execution.
     * This should match the method's max locals.
     */
    var maxLocals = 0

    /**
     * The maximum number of stack entries in this analysis execution.
     * This should match the method's max stack.
     */
    var maxStack = 0



    companion object {
        /**
         * An empty analysis result instance.
         */
        val EMPTY_RESULT = AnalyzerResult()
    }
}