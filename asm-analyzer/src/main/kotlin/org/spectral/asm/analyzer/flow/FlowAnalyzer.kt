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
import org.objectweb.asm.tree.*
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

                    /*
                     * Account for try-catch blocks
                     */
                    method.tryCatchBlocks.forEach loop1@ { tryCatchBlock ->
                        if(tryCatchBlock.start == currentLabel) {
                            var insertBefore: TryCatchBlockNode? = null
                            tryCatchNow.forEach loop2@ { tryCatchBlockNow ->
                                if(method.tryCatchBlocks.indexOf(tryCatchBlockNow) > method.tryCatchBlocks.indexOf(tryCatchBlock)) {
                                    insertBefore = tryCatchBlockNow
                                    return@loop2
                                }
                            }

                            if(insertBefore == null) {
                                tryCatchNow.add(tryCatchBlock)
                            } else {
                                tryCatchNow.add(tryCatchNow.indexOf(insertBefore), tryCatchBlock)
                            }
                        }

                        if(tryCatchBlock.end == currentLabel) {
                            tryCatchNow.remove(tryCatchBlock)
                        }
                    }

                    if(currentLabel != null) {
                        tryCatchMap[currentLabel!!] = tryCatchNow.let { mutableListOf<TryCatchBlockNode>().apply { this.addAll(it) } }
                    }

                    /*
                     * Continue
                     */
                    return@forEach
                }

                if(insn.opcode == Opcodes.GOTO || insn.opcode == Opcodes.TABLESWITCH || insn.opcode == Opcodes.LOOKUPSWITCH
                        || (insn.opcode >= Opcodes.IRETURN && insn.opcode <= Opcodes.RETURN) || insn.opcode == Opcodes.ATHROW) {
                    /*
                     * These opcodes signal an execution termination of the current method frame.
                     * Flag the control-flow block as crashed.
                     */
                    crashed = true

                    if(currentLabel == null) {
                        labels[ABSENT]!!.key.add(insn)
                    } else {
                        labels[currentLabel]!!.key.add(insn)
                    }
                }

                /*
                 * Build the flow graph of the label and tryCatch maps.
                 */
                labels.entries.forEach loop@ { entry ->
                    entry.value.key.forEach { ain ->
                        if(ain.opcode == Opcodes.GOTO) {
                            entry.value.value.add(Triple((ain as JumpInsnNode).label, JumpData(JumpCause.GOTO, ain), 0))
                            return@loop
                        }
                        else if(ain is JumpInsnNode) {
                            entry.value.value.add(Triple((ain as JumpInsnNode).label, JumpData(JumpCause.CONDITIONAL, ain), 0))
                        }
                        else if(ain is TableSwitchInsnNode || ain is LookupSwitchInsnNode) {
                            val jumps = if(ain is TableSwitchInsnNode) ain.labels else if(ain is LookupSwitchInsnNode) ain.labels else throw IllegalStateException()
                            val defaultHandler = if(ain is TableSwitchInsnNode) ain.dflt else if(ain is TableSwitchInsnNode) ain.dflt else throw IllegalStateException()

                            var i = 0
                            for(label: LabelNode in jumps) {
                                entry.value.value.add(Triple(label, JumpData(JumpCause.SWITCH, ain), i))
                                i++
                            }

                            entry.value.value.add(Triple(defaultHandler, JumpData(JumpCause.SWITCH, ain), i))
                            return@loop
                        }
                        else if((ain.opcode >= Opcodes.IRETURN && ain.opcode <= Opcodes.RETURN)
                                || ain.opcode == Opcodes.ATHROW) {
                            return@loop
                        }
                    }
                }
            }
        }

        labels.entries.forEach { entry ->
            if(entry.value.value.isNotEmpty() && entry.value.value[0].second.cause == JumpCause.NEXT) {
                entry.value.value.add(entry.value.value.removeAt(0))
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