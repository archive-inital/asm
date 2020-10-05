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
import com.google.common.collect.MultimapBuilder
import com.google.common.primitives.Primitives
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectral.asm.analyzer.method.frame.*
import org.spectral.asm.analyzer.method.value.ValueType
import org.spectral.asm.analyzer.util.PrimitiveUtils
import org.spectral.asm.core.*
import java.util.AbstractMap
import kotlin.math.max

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
        val stack = mutableListOf<StackObject?>()

        /*
         * The JVM local variable table of this method analysis.
         */
        val locals = mutableListOf<StackObject?>()

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
            stack: MutableList<StackObject?>,
            locals: MutableList<StackObject?>,
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
        var currentFrame: Frame

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
                NOP -> { currentFrame = Frame(NOP) }
                ACONST_NULL -> {
                    currentFrame = LdcFrame(insn.opcode, null)
                    stack.add(0, StackObject(Any::class, currentFrame, "java/lang/Object"))
                }
                in ICONST_M1..ICONST_5 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 3)
                    stack.add(0, StackObject(Int::class, currentFrame))
                }
                in LCONST_0..LCONST_1 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 9)
                    stack.add(0, StackObject(Long::class, currentFrame))
                }
                in FCONST_0..FCONST_2 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 11)
                    stack.add(0, StackObject(Float::class, currentFrame))
                }
                in DCONST_0..DCONST_1 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 14)
                    stack.add(0, StackObject(Double::class, currentFrame))
                }
                BIPUSH -> {
                    val cast = insn as IntInsnNode
                    currentFrame = LdcFrame(insn.opcode, cast.operand.toByte())
                    stack.add(0, StackObject(Byte::class, currentFrame))
                }
                SIPUSH -> {
                    val cast = insn as IntInsnNode
                    currentFrame = LdcFrame(insn.opcode, cast.operand.toShort())
                    stack.add(0, StackObject(Short::class, currentFrame))
                }
                LDC -> {
                    val cast = insn as LdcInsnNode
                    currentFrame = LdcFrame(insn.opcode, insn.cst)
                    var unwrapped: Class<*> = Primitives.unwrap(cast.cst.javaClass)
                    if(unwrapped == cast.cst.javaClass) {
                        unwrapped = if(cast.cst is Type) {
                            Class::class.java
                        } else {
                            cast.cst::class.java
                        }
                        stack.add(0, StackObject(Any::class, currentFrame, Type.getType(unwrapped).internalName))
                    } else {
                        stack.add(0, StackObject(unwrapped.kotlin, currentFrame))
                    }
                }
                in ILOAD..ALOAD -> {
                    val cast = insn as VarInsnNode
                    assureSize(locals, cast.`var`)
                    val local = locals[cast.`var`]!!
                    currentFrame = LocalFrame(insn.opcode, cast.`var`, local.value)
                    stack.add(0, local)
                }
                // TODO - Local Array Loading
                in ISTORE..ASTORE -> {
                    val cast = insn as VarInsnNode
                    val local = stack.removeAt(0)!!
                    currentFrame = LocalFrame(insn.opcode, cast.`var`, local.value)
                    assureSize(locals, cast.`var`)
                    locals[cast.`var`] = StackObject(local.type, currentFrame, local.initType)
                }
                // TODO - Local array storing
                -1 -> { currentFrame = NullFrame() }
                else -> throw RuntimeException("Unknown opcode ${insn.opcode}")
            }

            /*
             * Process the current frame.
             */
            if(currentFrame !is NullFrame) {
                val thisFrame = result.frames.put(insn, currentFrame).let { result.frames[insn] }
                result.mappings = null
                result.maxLocals = max(result.maxLocals, locals.size)
                result.maxStack = max(result.maxStack, locals.size)
            }

            /*
             * Process the handlers
             */
            handlers[insn]?.let { handler ->
                handler.forEach { tryCatchBlock ->
                    /*
                     * If we are at a handler instruction, jump to the handler execution frame.
                     * We jump back here afterwards.
                     */
                    if(jumps.add(insn to tryCatchBlock.handler)) {
                        val newStack = mutableListOf<StackObject?>()
                        newStack.add(0, StackObject(ArgumentFrame(-1, -1), if(tryCatchBlock == null) "java/lang/Throwable" else tryCatchBlock.type))
                        val newLocals = mutableListOf<StackObject?>()
                        newLocals.addAll(locals)
                        execute(method, tryCatchBlock.handler, newStack, newLocals, handlers, jumps, result)
                    }
                }
            }

            /*
             * Return if the execution has become terminal.
             */
            if(terminated) {
                return
            }

            /*
             * Process any branch successor instruction frames.
             */
            if(successors.isNotEmpty()) {
                successors.forEach { successor ->
                    if(jumps.add(insn to successor)) {
                        val newStack = mutableListOf<StackObject?>()
                        newStack.addAll(stack)
                        val newLocals = mutableListOf<StackObject?>()
                        newLocals.addAll(locals)
                        execute(method, successor, newStack, newLocals, handlers, jumps, result)
                    }
                }
                return
            } else {
                insn = insn.next
            }
        }
    }

    /**
     * Validates the provided list is of the required size by padding nulls to the end.
     *
     * @param list MutableList<StackObject?>
     * @param size Int
     */
    private fun assureSize(list: MutableList<StackObject?>, size: Int) {
        while(list.size <= size) {
            list.add(null)
        }
    }
}