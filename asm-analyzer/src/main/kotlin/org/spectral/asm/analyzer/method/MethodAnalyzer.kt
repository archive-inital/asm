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

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.primitives.Primitives
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectral.asm.analyzer.method.frame.ArgumentFrame
import org.spectral.asm.analyzer.method.frame.Frame
import org.spectral.asm.analyzer.method.frame.LdcFrame
import org.spectral.asm.analyzer.util.PrimitiveUtils
import org.spectral.asm.core.*
import kotlin.reflect.KClass

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

        /*
         * ============================================
         * START METHOD ANALYSIS
         * ============================================
         */

        /*
         * The JVM stack of this method analysis.
         */
        val stack = mutableListOf<StackObject>()

        /*
         * The JVM local variable table of this method analysis.
         */
        val locals = mutableListOf<StackObject>()

        /*
         * If the method is NOT static, add the 'this' entry as index 0
         * to the LVT.
         */
        var lvtIndex = 0
        if(!method.isStatic) {
            locals.add(StackObject(ArgumentFrame(ASTORE, 0), method.owner.name))
            lvtIndex++
        }

        /*
         * Add the arguments of the method being analyzed to the LVT.
         */
        method.argumentTypes.forEach { argType ->
            val typeClass = PrimitiveUtils.findPrimitive(argType.className)

            val opcode = when(typeClass) {
                Int::class -> ISTORE
                Long::class -> LSTORE
                Double::class -> DSTORE
                Float::class -> FSTORE
                else -> ASTORE
            }

            val frame = ArgumentFrame(opcode, ++lvtIndex)

            if(typeClass == null) {
                locals.add(StackObject(Any::class, frame, argType.internalName))
            } else {
                locals.add(StackObject(typeClass, frame))
            }

            /*
             * Account for wide data types (64bit length values)
             */
            if(typeClass == Double::class || typeClass == Long::class) {
                locals.add(StackObject(typeClass, frame))
            }
        }

        /*
         * Build a label map of try-catch handlers
         */
        val handlers = MultimapBuilder.hashKeys().arrayListValues().build<AbstractInsnNode, TryCatchBlockNode>()

        method.tryCatchBlocks?.forEach { tryCatchBlock ->
            var start: AbstractInsnNode = tryCatchBlock.start

            while(start != tryCatchBlock.end) {
                handlers.put(start, tryCatchBlock)
                start = start.next
            }
        }

        /*
         * Execute the method
         */
        execute(method, method.instructions.first, stack, locals, handlers, hashSetOf(), result)

        /*
         * ============================================
         * END METHOD ANALYSIS
         * ============================================
         */

        return result
    }

    /**
     * Runs a method execution flow simulation with the provided arguments.
     *
     * @param method MethodNode
     * @param initialInsn AbstractInsnNode
     * @param stack MutableList<StackObject>
     * @param locals MutableList<StackObject>
     * @param handlers ListMultimap<AbstractInsnNode, TryCatchBlockNode>
     * @param jumps HashSet<Pair<AbstractInsnNode, AbstractInsnNode>>
     * @param result AnalyzerResult
     */
    private fun execute(
            method: MethodNode,
            initialInsn: AbstractInsnNode,
            stack: MutableList<StackObject>,
            locals: MutableList<StackObject>,
            handlers: ListMultimap<AbstractInsnNode, TryCatchBlockNode>,
            jumps: HashSet<Pair<AbstractInsnNode, AbstractInsnNode>>,
            result: AnalyzerResult
    ) {
        /*
         * Whether the execution has reached a terminal opcode.
         */
        var terminated = false

        /*
         * The current instruction being executed.
         */
        var insn = initialInsn

        /*
         * The current instruction frame of the execution.
         */
        var frame: Frame

        /*
         * The successor instructions which branch this execution's control-flow.
         */
        val successors = mutableListOf<AbstractInsnNode>()

        /*
         * Loop forever. We will break out based on
         * various conditionals.
         */
        while(true) {
            /*
             * Switch through the opcodes
             */
            when(insn.opcode) {
                NOP -> frame = Frame(NOP)
                ACONST_NULL -> {
                    frame = LdcFrame(insn.opcode, null)
                    stack.add(0, StackObject(Any::class, frame, "java/lang/Object"))
                }
                in ICONST_M1..ICONST_5 -> {
                    frame = LdcFrame(insn.opcode, insn.opcode - 3)
                    stack.add(0, StackObject(Int::class, frame))
                }
                in LCONST_0..LCONST_1 -> {
                    frame = LdcFrame(insn.opcode, insn.opcode - 9)
                    stack.add(0, StackObject(Long::class, frame))
                }
                in FCONST_0..FCONST_2 -> {
                    frame = LdcFrame(insn.opcode, insn.opcode - 11)
                    stack.add(0, StackObject(Float::class, frame))
                }
                in DCONST_0..DCONST_1 -> {
                    frame = LdcFrame(insn.opcode, insn.opcode - 14)
                    stack.add(0, StackObject(Double::class, frame))
                }
                BIPUSH -> {
                    val cast = insn as IntInsnNode
                    frame = LdcFrame(insn.opcode, cast.operand.toByte())
                    stack.add(0, StackObject(Byte::class, frame))
                }
                SIPUSH -> {
                    val cast = insn as IntInsnNode
                    frame = LdcFrame(insn.opcode, cast.operand.toShort())
                    stack.add(0, StackObject(Short::class, frame))
                }
                LDC -> {
                    val cast = insn as LdcInsnNode
                    frame = LdcFrame(insn.opcode, insn.cst)
                    var unwrapped: Class<*> = Primitives.unwrap(cast.cst.javaClass)
                    if(unwrapped == cast.cst.javaClass) {
                        unwrapped = if(cast.cst is Type) {
                            Class::class.java
                        } else {
                            cast.cst::class.java
                        }
                        stack.add(0, StackObject(Any::class, frame, Type.getType(unwrapped).internalName))
                    } else {
                        stack.add(0, StackObject(unwrapped.kotlin, frame))
                    }
                }
                else -> throw RuntimeException("Unknown opcode ${insn.opcode}")
            }
        }
    }
}