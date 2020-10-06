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
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max
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
     * Executes an array load operation.
     *
     * @param opcode Int
     * @param stack MutableList<StackObject?>
     * @param type KClass<*>
     * @return Frame
     */
    private fun executeArrayLoad(opcode: Int, stack: MutableList<StackObject?>, type: KClass<*>): Frame {
        val index = stack.removeAt(0)!!.value!!
        val array = stack.removeAt(0)!!.value!!

        val currentFrame = ArrayLoadFrame(opcode, index, array)

        if(type == Any::class) {
            stack.add(0, StackObject(type, currentFrame, "java/lang/Object"))
        } else {
            stack.add(0, StackObject(type, currentFrame))
        }

        return currentFrame
    }

    /**
     * Executes an array store operation.
     *
     * @param opcode Int
     * @param stack MutableList<StackObject?>
     * @return Frame
     */
    private fun executeArrayStore(opcode: Int, stack: MutableList<StackObject?>): Frame {
        val value = stack.removeAt(0)!!.value!!
        val index = stack.removeAt(0)!!.value!!
        val array = stack.removeAt(0)!!.value!!
        return ArrayStoreFrame(opcode, value, index, array)
    }

    /**
     * Execute an unary math operation on the stack.
     *
     * @param opcode Int
     * @param stack MutableList<StackObject?>
     * @param prim KClass<P>
     * @return Frame
     */
    private fun <P : Any> doUnaryMath(opcode: Int, stack: MutableList<StackObject?>, prim: KClass<P>): Frame {
        val target = stack.removeAt(0)!!.value!!
        val currentFrame = MathFrame(opcode, target)
        stack.add(0, StackObject(prim, currentFrame))
        return currentFrame
    }

    /**
     * Executes a binary math operation on the stack.
     *
     * @param opcode Int
     * @param stack MutableList<StackObject?>
     * @param prim KClass<P>
     * @return Frame
     */
    private fun <P : Any> doBinaryMath(opcode: Int, stack: MutableList<StackObject?>, prim: KClass<P>): Frame {
        val target1 = stack.removeAt(0)!!.value!!
        val target2 = stack.removeAt(0)!!.value!!
        val currentFrame = MathFrame(opcode, target1, target2)
        stack.add(0, StackObject(prim, currentFrame))
        return currentFrame
    }

    /**
     * Executes a casting operation on the stack.
     *
     * @param opcode Int
     * @param stack MutableList<StackObject?>
     * @param prim KClass<*>
     * @return Frame
     */
    private fun doCast(opcode: Int, stack: MutableList<StackObject?>, prim: KClass<*>): Frame {
        val value = stack.removeAt(0)!!.value!!
        val currentFrame = MathFrame(opcode, value)
        stack.add(0, StackObject(prim, currentFrame))
        return currentFrame
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
                IALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Int::class) }
                LALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Long::class) }
                FALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Float::class) }
                DALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Double::class) }
                AALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Any::class) }
                BALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Byte::class) }
                CALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Char::class) }
                SALOAD -> { currentFrame = executeArrayLoad(insn.opcode, stack, Short::class) }
                in ISTORE..ASTORE -> {
                    val cast = insn as VarInsnNode
                    val local = stack.removeAt(0)!!
                    currentFrame = LocalFrame(insn.opcode, cast.`var`, local.value)
                    assureSize(locals, cast.`var`)
                    locals[cast.`var`] = StackObject(local.type, currentFrame, local.initType)
                }
                in IASTORE..AASTORE -> {
                    currentFrame = executeArrayStore(insn.opcode, stack)
                }
                POP -> {
                    val value = stack.removeAt(0)!!
                    currentFrame = PopFrame(insn.opcode, value.value!!)
                }
                POP2 -> {
                    val obj = stack[0]!!
                    currentFrame = if(obj.type == Double::class || obj.type == Long::class) {
                        stack.removeAt(0)
                        PopFrame(insn.opcode, obj.value!!)
                    } else {
                        stack.removeAt(0)
                        val next = stack.removeAt(0)!!
                        PopFrame(insn.opcode, obj.value!!, next.value!!)
                    }
                }
                DUP -> {
                    val obj = stack[0]!!
                    currentFrame = DupFrame(insn.opcode, obj.value!!)
                    stack.add(0, obj)
                }
                DUP_X1 -> {
                    val obj = stack[0]!!
                    if(obj.type == Double::class || obj.type == Long::class) {
                        throw IllegalArgumentException()
                    }
                    stack.add(2, obj)
                    currentFrame = DupFrame(insn.opcode, obj.value!!)
                }
                DUP_X2 -> {
                    val obj = stack[1]!!
                    val first = stack[0]!!
                    currentFrame = DupFrame(insn.opcode, first.value!!)
                    if(obj.type == Double::class || obj.type == Long::class) {
                        stack.add(2, stack[0]!!)
                    } else {
                        stack.add(3, stack[0]!!)
                    }
                }
                DUP2 -> {
                    val o = stack[0]!!
                    val obj = stack[0]!!
                    currentFrame = if(obj.type == Double::class || obj.type == Long::class) {
                        stack.add(1, o)
                        DupFrame(insn.opcode, o.value!!)
                    } else {
                        val o1 = stack[1]!!
                        stack.add(2, o)
                        stack.add(3, o1)
                        DupFrame(insn.opcode, o.value!!, o1.value!!)
                    }
                }
                DUP2_X1 -> {
                    val o = stack[0]!!
                    val obj = stack[0]!!
                    currentFrame = if(obj.type == Double::class || obj.type == Long::class) {
                        stack.add(2, o)
                        DupFrame(insn.opcode, o.value!!)
                    } else {
                        val o1 = stack[1]!!
                        stack.add(3, o)
                        stack.add(4, o1)
                        DupFrame(insn.opcode, o.value!!, o1.value!!)
                    }
                }
                DUP2_X2 -> {
                    val o = stack[0]!!
                    var obj = stack[0]!!
                    if(obj.type == Double::class || obj.type == Long::class) {
                        obj = stack[1]!!
                        currentFrame = DupFrame(insn.opcode, o.value!!)
                        if(obj.type == Double::class || obj.type == Long::class) {
                            stack.add(2, o)
                        } else {
                            stack.add(3, o)
                        }
                    } else {
                        val o1 = stack[1]!!
                        obj = stack[2]!!
                        currentFrame = DupFrame(insn.opcode, o.value!!, o1.value!!)
                        if(obj.type == Double::class || obj.type == Long::class) {
                            stack.add(3, o)
                            stack.add(4, o1)
                        } else {
                            stack.add(4, o)
                            stack.add(5, o1)
                        }
                    }
                }
                SWAP -> {
                    val top = stack.removeAt(0)!!
                    val bottom = stack.removeAt(0)!!
                    currentFrame = SwapFrame(top.value!!, bottom.value!!)
                    stack.add(0, top)
                    stack.add(0, bottom)
                }
                IADD,
                ISUB,
                IMUL,
                IDIV,
                IREM,
                ISHL,
                ISHR,
                IUSHR,
                IAND,
                IOR,
                IXOR,
                LCMP,
                FCMPL,
                FCMPG,
                DCMPL,
                DCMPG -> {
                    currentFrame = doBinaryMath(insn.opcode, stack, Int::class)
                }
                LADD,
                LSUB,
                LMUL,
                LDIV,
                LREM,
                LSHL,
                LSHR,
                LUSHR,
                LAND,
                LOR,
                LXOR -> {
                    currentFrame = doBinaryMath(insn.opcode, stack, Long::class)
                }
                FADD,
                FSUB,
                FMUL,
                FDIV,
                FREM -> {
                    currentFrame = doBinaryMath(insn.opcode, stack, Float::class)
                }
                DADD,
                DSUB,
                DMUL,
                DDIV,
                DREM -> {
                    currentFrame = doBinaryMath(insn.opcode, stack, Double::class)
                }
                INEG -> {
                    currentFrame = doUnaryMath(insn.opcode, stack, Int::class)
                }
                LNEG -> {
                    currentFrame = doUnaryMath(insn.opcode, stack, Long::class)
                }
                FNEG -> {
                    currentFrame = doUnaryMath(insn.opcode, stack, Float::class)
                }
                DNEG -> {
                    currentFrame = doUnaryMath(insn.opcode, stack, Double::class)
                }
                IINC -> {
                    val cast = insn as IincInsnNode
                    assureSize(locals, cast.`var`)
                    val local = locals[cast.`var`]!!
                    currentFrame = LocalFrame(insn.opcode, cast.`var`, local.value!!)
                }
                I2L,
                F2L,
                D2L -> {
                    currentFrame = doCast(insn.opcode, stack, Long::class)
                }
                I2F,
                L2F,
                D2F -> {
                    currentFrame = doCast(insn.opcode, stack, Float::class)
                }
                I2D,
                L2D,
                F2D -> {
                    currentFrame = doCast(insn.opcode, stack, Double::class)
                }
                L2I,
                D2I,
                F2I -> {
                    currentFrame = doCast(insn.opcode, stack, Int::class)
                }
                I2B -> {
                    currentFrame = doCast(insn.opcode, stack, Byte::class)
                }
                I2C -> {
                    currentFrame = doCast(insn.opcode, stack, Char::class)
                }
                I2S -> {
                    currentFrame = doCast(insn.opcode, stack, Short::class)
                }
                IFEQ,
                IFNE,
                IFLT,
                IFGE,
                IFGT,
                IFLE,
                IFNULL,
                IFNONNULL -> {
                    val cast = insn as JumpInsnNode
                    val obj = stack.removeAt(0)!!.value!!
                    successors.add(cast.label)
                    successors.add(insn.next)
                    currentFrame = JumpFrame(insn.opcode, listOf(obj), cast.label, insn.next)
                }
                IF_ICMPEQ,
                IF_ICMPNE,
                IF_ICMPLT,
                IF_ICMPGT,
                IF_ICMPGE,
                IF_ICMPLE,
                IF_ACMPNE,
                IF_ACMPEQ -> {
                    val cast = insn as JumpInsnNode
                    val obj1 = stack.removeAt(0)!!.value!!
                    val obj2 = stack.removeAt(0)!!.value!!
                    successors.add(cast.label)
                    successors.add(insn.next)
                    currentFrame = JumpFrame(insn.opcode, listOf(obj1, obj2), cast.label, insn.next)
                }
                GOTO -> {
                    val cast = insn as JumpInsnNode
                    successors.add(cast.label)
                    currentFrame = JumpFrame(insn.opcode, emptyList(), cast.label)
                }
                JSR -> { throw UnsupportedOperationException("JSR not supported in Java 1.8+") }
                RET -> { throw UnsupportedOperationException("RET not supported in Java 1.8+") }
                TABLESWITCH -> {
                    val frame = stack.removeAt(0)!!.value!!
                    val cast = insn as TableSwitchInsnNode
                    currentFrame = SwitchFrame(insn.opcode, frame, cast.labels, cast.dflt)
                    successors.addAll(cast.labels)
                    successors.add(cast.dflt)
                }
                LOOKUPSWITCH -> {
                    val frame = stack.removeAt(0)!!.value!!
                    val cast = insn as LookupSwitchInsnNode
                    currentFrame = SwitchFrame(insn.opcode, frame, cast.labels, cast.dflt)
                    successors.addAll(cast.labels)
                    successors.add(cast.dflt)
                }
                -1 -> { currentFrame = NullFrame() }
                else -> throw RuntimeException("Unknown opcode ${insn.opcode}")
            }

            /*
             * Process the current frame.
             */
            if(currentFrame !is NullFrame) {
                result.frames.put(insn, currentFrame)
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