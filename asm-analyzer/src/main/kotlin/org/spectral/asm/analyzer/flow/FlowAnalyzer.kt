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

package org.spectral.asm.analyzer.flow

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import java.util.AbstractMap

/**
 * Analyzes the control-flow of a method execution.
 *
 * @property method MethodNode
 * @constructor
 */
class FlowAnalyzer(val method: MethodNode) {

    /**
     * Run the flow analysis on the [method].
     *
     * @return FlowAnalyzerResult
     */
    fun analyze(): FlowAnalyzerResult {
        /**
         * The label jump graph.
         */
        val labels = LinkedHashMap<LabelNode, Map.Entry<MutableList<AbstractInsnNode>, MutableList<Triple<LabelNode, JumpData, Int>>>>()

        /**
         * The try-catch block jump graph.
         */
        val tryCatchMap = LinkedHashMap<LabelNode, MutableList<TryCatchBlockNode>>()

        /**
         * Whether the flow has reach any problems
         */
        var crashed = false
        var currentLabel: LabelNode? = null

        labels.putIfAbsent(ABSENT, AbstractMap.SimpleEntry(mutableListOf(), mutableListOf()))
        val tryCatchNow = mutableListOf<TryCatchBlockNode>()

        method.instructions.toArray().forEach { insn ->
            if(insn.opcode == Opcodes.RET || insn.opcode == Opcodes.JSR) {
                throw UnsupportedOperationException("JSR/RET not supported in JVM 1.8+.")
            }

            if(insn is LabelNode) {
                if(!crashed) {
                    if(currentLabel == null) {
                        labels[ABSENT]!!.value.add(Triple(insn, JumpData(JumpCause.NEXT, null), 0))
                    } else {
                        labels[currentLabel]!!.value.add(Triple(insn, JumpData(JumpCause.NEXT, null), 0))
                    }
                    crashed = false
                    currentLabel = insn
                    labels.putIfAbsent(currentLabel!!, AbstractMap.SimpleEntry(mutableListOf(), mutableListOf()))
                }
            }
        }

        return FlowAnalyzerResult(labels, tryCatchMap)
    }

    companion object {
        /**
         * The label used when no label is found.
         */
        val ABSENT = LabelNode()
    }
}