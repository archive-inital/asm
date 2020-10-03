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

import com.google.common.primitives.Primitives
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.spectral.asm.analyzer.frame.*
import org.spectral.asm.analyzer.util.PrimitiveUtils
import org.spectral.asm.analyzer.value.Value
import org.spectral.asm.analyzer.value.ValueType
import org.spectral.asm.core.Method
import org.spectral.asm.core.code.Instruction
import org.spectral.asm.core.code.type.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.math.max
import kotlin.reflect.KClass
import org.spectral.asm.core.code.Exception as ExceptionBlock

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
        result.frames = LinkedHashMap()

        /**
         * A list of execution stack states or (contexts) at each execution frame (each instruction).
         */
        val stack = mutableListOf<StackContext>()

        /**
         * A list of execution LVT states or (contexts) at each execution frame (each instruction).
         */
        val locals = mutableListOf<StackContext?>()

        /**
         * The current local variable index. This value should always be '<size of LVT> - 1'.
         */
        var lvtIndex = 0

        /*
         * If the method is NOT static, we need to add the 'this' local variable as an ASTORE instruction to store
         * it's reference to the LVT in the stack.
         */
        if(!method.isStatic) {
            locals.add(StackContext(ArgumentFrame(Opcodes.ASTORE, 0), method.owner.name))
            lvtIndex++
        }

        /*
         * Load all the argument types from the method onto the LVT.
         */
        method.argumentTypes.forEach { type ->
            val klass = PrimitiveUtils.forName(type.className)

            val opcode = when(klass) {
                Int::class -> ISTORE
                Long::class -> LSTORE
                Double::class -> DSTORE
                Float::class -> FSTORE
                else -> ASTORE
            }

            val frame = ArgumentFrame(opcode, lvtIndex)

            if(klass == null) {
                locals.add(StackContext(Any::class, frame, type.internalName))
            } else {
                locals.add(StackContext(klass, frame))
            }

            /*
             * Account for 64bit wide stack values. (Doubles and longs)
             */
            if(klass == Double::class || klass == Long::class) {
                locals.add(StackContext(klass, frame))
            }
        }

        /*
         * Deal with how exception handling is done per Oracle JVM specifications.
         * This is how Java 8+ handles exceptions.
         */

        /**
         * A map of instruction -> exception objects.
         * Contains a list of instruction and what exception blocks they are apart of.
         */
        val handlers = hashMapOf<Instruction, MutableList<ExceptionBlock>>()

        /*
         * If the method has exception try-catch blocks.
         */
        if(method.code.exceptions.isNotEmpty()) {
            method.code.exceptions.forEach { exceptionBlock ->
                var insn: Instruction? = exceptionBlock.start
                while(insn != exceptionBlock.end) {
                    handlers.computeIfAbsent(insn!!) { mutableListOf() }.add(exceptionBlock)
                    insn = insn.next
                }
            }
        }

        /*
         * Execute the method starting at the method's first instruction.
         */
        try {
            this.execute(method, method.code.instructions.first, stack, locals, handlers, hashSetOf(), result)
        } catch(e : StackOverflowError) {
            /*
             * We do not want to cause any stack overflows.
             */
            throw RuntimeException("Stack overflow. Maximum stack size is ${method.code.maxStack}.")
        }

        return result
    }

    /**
     * Executes an array load of a given type on the provided stack.
     *
     * @param opcode Int
     * @param stack MutableList<StackContext>
     * @param type KClass<*>
     * @return Frame
     */
    private fun executeArrayLoad(opcode: Int, stack: MutableList<StackContext>, type: KClass<*>): Frame {
        val index = stack.pop().value
        val array = stack.pop().value

        val currentFrame = ArrayLoadFrame(opcode, index!!, array!!)

        if(type == Any::class) {
            stack.push(StackContext(type, currentFrame, "java/lang/Object")) // Fuck this. We really should unwrap all types.
        } else {
            stack.push(StackContext(type, currentFrame))
        }

        return currentFrame
    }

    /**
     * Executes an array store for a given opcode on the provided stack.
     *
     * @param opcode Int
     * @param stack MutableList<StackContext>
     * @return Frame
     */
    private fun executeArrayStore(opcode: Int, stack: MutableList<StackContext>): Frame {
        val value = stack.pop().value!!
        val index = stack.pop().value!!
        val array = stack.pop().value!!
        return ArrayStoreFrame(opcode, value, index, array)
    }

    /**
     * Executes a stack cast instruction on the given stack and return a casted [Frame].
     *
     * @param opcode Int
     * @param stack MutableList<StackContext>
     * @param primitive KClass<*>
     * @return Frame
     */
    private fun doCast(opcode: Int, stack: MutableList<StackContext>, primitive: KClass<*>): Frame {
        val value = stack.pop().value!!
        val currentFrame = MathFrame(opcode, value)
        stack.push(StackContext(primitive, currentFrame))
        return currentFrame
    }

    /**
     * Executes unary stack math operation on the provided [stack].
     *
     * @param opcode Int
     * @param stack MutableList<StackContext>
     * @param primitive KClass<P>
     * @return Frame
     */
    private fun <P : Any> doUnaryMath(opcode: Int, stack: MutableList<StackContext>, primitive: KClass<P>): Frame {
        val target = stack.pop().value!!
        val currentFrame = MathFrame(opcode, target)
        stack.push(StackContext(primitive, currentFrame))
        return currentFrame
    }

    /**
     * Executes binary stack math operation on the provided stack and returns the resulting math frame.
     *
     * @param opcode Int
     * @param stack MutableList<StackContext>
     * @param primitive KClass<P>
     * @return Frame
     */
    private fun <P : Any> doBinaryMath(opcode: Int, stack: MutableList<StackContext>, primitive: KClass<P>): Frame {
        val target1 = stack.pop().value!!
        val target2 = stack.pop().value!!
        val currentFrame = MathFrame(opcode, target1, target2)
        stack.push(StackContext(primitive, currentFrame))
        return currentFrame
    }

    /**
     * Executes a instruction from a method and updates the provided data maps and analysis result values.
     *
     * @param method Method
     * @param initialInsn Instruction
     * @param stack MutableList<StackContext>
     * @param locals MutableList<StackContext>
     * @param handlers HashMap<Instruction, MutableList<Exception>>
     * @param jumps MutableSet<Entry<Instruction, Instruction>>
     * @param result AnalyzerResult
     */
    private fun execute(
            method: Method,
            initialInsn: Instruction,
            stack: MutableList<StackContext>,
            locals: MutableList<StackContext?>,
            handlers: HashMap<Instruction, MutableList<ExceptionBlock>>,
            jumps: MutableSet<Map.Entry<Instruction, Instruction>>,
            result: AnalyzerResult
    ) {
        /**
         * The current instruction being executed.
         */
        var insn = initialInsn

        /**
         * Whether the execution is complete.
         */
        var complete = false

        /**
         * The successing instructions for execution. We can have multiple in the event of a logical branch
         * in the code.
         */
        val successors = mutableListOf<Instruction>()

        /**
         * The current execution frame being executed.
         */
        var currentFrame: Frame? = null

        /*
         * Loop until we break out.
         */
        while(true) {
            /*
             * Determine the action based on the executing instruction opcode.
             * Based on the opcode and what it does, we instantiate the [currentFrame] appropriately.
             */
            when(insn.opcode) {
                -1 -> {
                    currentFrame = null
                }
                NOP -> { currentFrame = Frame(NOP) }
                ACONST_NULL -> {
                    currentFrame = LdcFrame(insn.opcode, null)
                    stack.push(StackContext(Any::class, currentFrame, "java/lang/Object"))
                }
                ICONST_M1,
                ICONST_0,
                ICONST_1,
                ICONST_2,
                ICONST_3,
                ICONST_4,
                ICONST_5 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 3)
                    stack.push(StackContext(Int::class, currentFrame))
                }
                LCONST_0,
                LCONST_1 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 9)
                    stack.push(StackContext(Long::class, currentFrame))
                }
                FCONST_0,
                FCONST_1,
                FCONST_2 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 11)
                    stack.push(StackContext(Float::class, currentFrame))
                }
                DCONST_0,
                DCONST_1 -> {
                    currentFrame = LdcFrame(insn.opcode, insn.opcode - 14)
                    stack.push(StackContext(Double::class, currentFrame))
                }
                BIPUSH -> {
                    val cast = insn as IntInstruction
                    currentFrame = LdcFrame(insn.opcode, cast.operand.toByte())
                    stack.push(StackContext(Byte::class, currentFrame))
                }
                SIPUSH -> {
                    val cast = insn as IntInstruction
                    currentFrame = LdcFrame(insn.opcode, cast.operand.toShort())
                    stack.push(StackContext(Short::class, currentFrame))
                }
                LDC -> {
                    val cast = insn as LdcInstruction
                    currentFrame = LdcFrame(insn.opcode, cast.cst)
                    var unwrapped = Primitives.unwrap(cast.cst::class.java).kotlin
                    if(unwrapped == cast.cst::class) {
                        if(cast.cst is Type) {
                            unwrapped = Class::class
                        } else {
                            unwrapped = cast.cst::class
                        }
                        stack.push(StackContext(Any::class, currentFrame, Type.getType(unwrapped.java).internalName))
                    } else {
                        stack.push(StackContext(unwrapped, currentFrame))
                    }
                }
                ILOAD,
                LLOAD,
                FLOAD,
                DLOAD,
                ALOAD -> {
                    val cast = insn as LVTInstruction
                    locals.assureSize(cast.index)
                    val ctx = locals[cast.index]!!
                    currentFrame = LocalFrame(insn.opcode, cast.index, ctx.value)
                    stack.push(ctx)
                }
                IALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Int::class)
                LALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Long::class)
                FALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Float::class)
                DALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Double::class)
                AALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Any::class)
                BALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Byte::class)
                CALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Char::class)
                SALOAD -> currentFrame = executeArrayLoad(insn.opcode, stack, Short::class)
                ISTORE,
                LSTORE,
                FSTORE,
                DSTORE,
                ASTORE -> {
                    val cast = insn as LVTInstruction
                    val ctx = stack.pop()
                    currentFrame = LocalFrame(insn.opcode, cast.index, ctx.value)
                    locals.assureSize(cast.index)
                    locals[cast.index] = StackContext(ctx.type, currentFrame, ctx.initType)
                }
                IASTORE,
                LASTORE,
                FASTORE,
                DASTORE,
                BASTORE,
                CASTORE,
                SASTORE,
                AASTORE -> {
                    currentFrame = executeArrayStore(insn.opcode, stack)
                }
                POP -> {
                    val ctx = stack.pop()
                    currentFrame = PopFrame(insn.opcode, ctx.value)
                }
                POP2 -> {
                    val ctx = stack.first()
                    currentFrame = if(ctx.type == Double::class || ctx.type == Long::class) {
                        stack.pop()
                        PopFrame(insn.opcode, ctx.value)
                    } else {
                        stack.pop()
                        val next = stack.pop()
                        PopFrame(insn.opcode, ctx.value, next.value)
                    }
                }
                DUP -> {
                    val ctx = stack.first()
                    currentFrame = DupFrame(insn.opcode, ctx.value)
                    stack.push(ctx)
                }
                DUP_X1 -> {
                    val ctx = stack.first()
                    if(ctx.type == Double::class || ctx.type == Long::class) {
                        throw IllegalStateException()
                    }

                    stack.add(2, ctx)
                    currentFrame = DupFrame(insn.opcode, ctx.value)
                }
                DUP_X2 -> {
                    val ctx = stack[1]
                    val top = stack.first()
                    currentFrame = DupFrame(insn.opcode, top.value)
                    if(ctx.type == Double::class || ctx.type == Long::class) {
                        stack.add(2, stack.first())
                    } else {
                        stack.add(3, stack.first())
                    }
                }
                DUP2 -> {
                    val ctx = stack.first()
                    currentFrame = if(ctx.type == Double::class || ctx.type == Long::class) {
                        stack.add(1, ctx)
                        DupFrame(insn.opcode, ctx.value)
                    } else {
                        val top = stack[1]
                        stack.add(2, ctx)
                        stack.add(3, top)
                        DupFrame(insn.opcode, ctx.value, top.value)
                    }
                }
                DUP2_X1 -> {
                    val ctx = stack.first()
                    currentFrame = if(ctx.type == Double::class || ctx.type == Long::class) {
                        stack.add(2, ctx)
                        DupFrame(insn.opcode, ctx.value)
                    } else {
                        val top = stack[1]
                        stack.add(3, ctx)
                        stack.add(4, top)
                        DupFrame(insn.opcode, ctx.value, top.value)
                    }
                }
                DUP2_X2 -> {
                    var ctx = stack.first()
                    if(ctx.type == Double::class || ctx.type == Long::class) {
                        ctx = stack[1]
                        currentFrame = DupFrame(insn.opcode, stack.first().value)
                        if(ctx.type == Double::class || ctx.type == Long::class) {
                            stack.add(2, stack.first())
                        } else {
                            stack.add(3, stack.first())
                        }
                    } else {
                        val top = stack[1]
                        ctx = stack[2]
                        currentFrame = DupFrame(insn.opcode, stack.first().value, top.value)
                        if(ctx.type == Double::class || ctx.type == Long::class) {
                            stack.add(3, stack.first())
                            stack.add(4, top)
                        } else {
                            stack.add(4, stack.first())
                            stack.add(5, top)
                        }
                    }
                }
                SWAP -> {
                    val top = stack.pop()
                    val bottom = stack.pop()
                    currentFrame = SwapFrame(insn.opcode, top.value, bottom.value)
                    stack.push(top)
                    stack.push(bottom)
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
                INEG -> currentFrame = doUnaryMath(insn.opcode, stack, Int::class)
                LNEG -> currentFrame = doUnaryMath(insn.opcode, stack, Long::class)
                FNEG -> currentFrame = doUnaryMath(insn.opcode, stack, Float::class)
                DNEG -> currentFrame = doUnaryMath(insn.opcode, stack, Double::class)
                IINC -> {
                    val cast = insn as IncInstruction
                    locals.assureSize(cast.index)
                    val ctx = locals[cast.index]!!
                    currentFrame = LocalFrame(insn.opcode, cast.index, ctx.value)
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
                I2B -> currentFrame = doCast(insn.opcode, stack, Byte::class)
                I2C -> currentFrame = doCast(insn.opcode, stack, Char::class)
                I2S -> currentFrame = doCast(insn.opcode, stack, Short::class)
                IFEQ,
                IFNE,
                IFLT,
                IFGE,
                IFGT,
                IFLE,
                IFNULL,
                IFNONNULL -> {
                    val cast = insn as JumpInstruction
                    val target = stack.pop().value!!
                    successors.add(cast.label)
                    successors.add(insn.next!!)
                    currentFrame = JumpFrame(insn.opcode, listOf(target), cast.label, insn.next!!)
                }
                IF_ICMPEQ,
                IF_ICMPNE,
                IF_ICMPLT,
                IF_ICMPGT,
                IF_ICMPGE,
                IF_ICMPLE,
                IF_ACMPNE,
                IF_ACMPEQ -> {
                    val cast = insn as JumpInstruction
                    val target1 = stack.pop().value!!
                    val target2 = stack.pop().value!!
                    successors.add(cast.label)
                    successors.add(insn.next!!)
                    currentFrame = JumpFrame(insn.opcode, listOf(target1, target2), cast.label, insn.next!!)
                }
                GOTO -> {
                    val cast = insn as JumpInstruction
                    successors.add(cast.label)
                    currentFrame = JumpFrame(insn.opcode, emptyList(), cast.label)
                }
                JSR -> throw UnsupportedOperationException("JSR opcode not supported in JVM 8+")
                RET -> throw UnsupportedOperationException("RET opcode not supported in JVM 8+")
                TABLESWITCH -> {
                    val frame = stack.pop().value!!
                    val cast = insn as TableSwitchInstruction
                    currentFrame = SwitchFrame(insn.opcode, frame, cast.labels, cast.default!!)
                    successors.addAll(cast.labels)
                    successors.add(cast.default!!)
                }
                LOOKUPSWITCH -> {
                    val frame = stack.pop().value!!
                    val cast = insn as LookupSwitchInstruction
                    currentFrame = SwitchFrame(insn.opcode, frame, cast.labels, cast.default!!)
                    successors.addAll(cast.labels)
                    successors.add(cast.default!!)
                }
                IRETURN,
                LRETURN,
                FRETURN,
                DRETURN,
                ARETURN -> {
                    currentFrame = ReturnFrame(insn.opcode, stack.pop().value!!)
                    complete = true
                }
                RETURN -> {
                    currentFrame = ReturnFrame(insn.opcode, null)
                    complete = true
                }
                GETSTATIC -> {
                    val cast = insn as FieldInstruction
                    val type = Type.getType(cast.field.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    currentFrame = FieldFrame(insn.opcode, cast.field.owner.name, cast.field.name, cast.field.desc, null, null)
                    if(klass == null) {
                        stack.push(StackContext(Any::class, currentFrame, type.internalName))
                    } else {
                        stack.push(StackContext(klass, currentFrame))
                    }
                }
                PUTSTATIC -> {
                    val cast = insn as FieldInstruction
                    currentFrame = FieldFrame(insn.opcode, cast.field.owner.name, cast.field.name, cast.field.desc, null, stack.pop().value!!)
                }
                GETFIELD -> {
                    val cast = insn as FieldInstruction
                    val type = Type.getType(cast.field.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    currentFrame = FieldFrame(insn.opcode, cast.field.owner.name, cast.field.name, cast.field.desc, stack.pop().value!!, null)
                    if(klass == null) {
                        stack.push(StackContext(Any::class, currentFrame, type.internalName))
                    } else {
                        stack.push(StackContext(klass, currentFrame))
                    }
                }
                PUTFIELD -> {
                    val cast = insn as FieldInstruction
                    val obj = stack.pop().value
                    val instance = stack.pop().value
                    currentFrame = FieldFrame(insn.opcode, cast.field.owner.name, cast.field.name, cast.field.desc, instance, obj)
                }
                INVOKEVIRTUAL -> {
                    val cast = insn as MethodInstruction
                    val type = Type.getReturnType(cast.method.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    val args = mutableListOf<Frame?>()
                    Type.getArgumentTypes(cast.method.desc).forEach { _ ->
                        args.add(0, stack.pop().value)
                    }
                    currentFrame = MethodFrame(insn.opcode, cast.method.owner.name, cast.method.name, cast.method.desc, stack.pop().value, args)
                    if(type.sort != Type.VOID) {
                        if(klass == null) {
                            stack.push(StackContext(Any::class, currentFrame, type.internalName))
                        } else {
                            stack.push(StackContext(klass, currentFrame))
                        }
                    }
                }
                INVOKESPECIAL -> {
                    val cast = insn as MethodInstruction
                    val type = Type.getReturnType(cast.method.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    val args = mutableListOf<Frame?>()
                    Type.getArgumentTypes(cast.method.desc).forEach { _ ->
                        args.add(0, stack.pop().value!!)
                    }
                    val instance = stack.pop()
                    instance.initialize()
                    currentFrame = MethodFrame(insn.opcode, cast.method.owner.name, cast.method.name, cast.method.desc, instance.value, args)
                    if(type.sort != Type.VOID) {
                        if(klass == null) {
                            stack.push(StackContext(Any::class, currentFrame, type.internalName))
                        } else {
                            stack.push(StackContext(klass, currentFrame))
                        }
                    }
                }
                INVOKESTATIC -> {
                    val cast = insn as MethodInstruction
                    val type = Type.getReturnType(cast.method.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    val args = mutableListOf<Frame?>()
                    Type.getArgumentTypes(cast.method.desc).forEach {
                        args.add(0, stack.pop().value)
                    }
                    currentFrame = MethodFrame(insn.opcode, cast.method.owner.name, cast.method.name, cast.method.desc, null, args)
                    if(type.sort != Type.VOID) {
                        if(klass == null) {
                            stack.push(StackContext(Any::class, currentFrame, type.internalName))
                        } else {
                            stack.push(StackContext(klass, currentFrame))
                        }
                    }
                }
                INVOKEINTERFACE -> {
                    val cast = insn as MethodInstruction
                    val type = Type.getReturnType(cast.method.desc)
                    val klass = PrimitiveUtils.forName(type.className)
                    val args = mutableListOf<Frame?>()
                    Type.getArgumentTypes(cast.method.desc).forEach {
                        args.add(0, stack.pop().value)
                    }
                    currentFrame = MethodFrame(insn.opcode, cast.method.owner.name, cast.method.name, cast.method.desc, stack.pop().value, args)
                    if(type.sort != Type.VOID) {
                        if(klass == null) {
                            stack.push(StackContext(Any::class, currentFrame, type.internalName))
                        } else {
                            stack.push(StackContext(klass, currentFrame))
                        }
                    }
                }
                NEW -> {
                    val cast = insn as TypeInstruction
                    currentFrame = NewFrame(cast.type.name)
                    stack.push(StackContext(currentFrame))
                }
                NEWARRAY -> {
                    val length = stack.pop().value!!
                    val cast = insn as IntInstruction
                    currentFrame = NewArrayFrame(insn.opcode, PrimitiveUtils.forArrayId(cast.operand).simpleName!!, length)
                    val desc = "[" + Type.getType(PrimitiveUtils.forArrayId(cast.operand).java).descriptor
                    stack.push(StackContext(Any::class, currentFrame, desc))
                }
                ARRAYLENGTH -> {
                    val array = stack.pop().value!!
                    currentFrame = ArrayLengthFrame(array)
                    stack.push(StackContext(Int::class, currentFrame))
                }
                ATHROW -> {
                    val throwable = stack.pop().value!!
                    currentFrame = ThrowFrame(throwable)
                    complete = true
                }
                CHECKCAST -> {
                    val cast = insn as TypeInstruction
                    val obj = StackContext(Any::class, stack.first().value!!, cast.type.name)
                    stack.pop()
                    stack.push(obj)
                    currentFrame = CheckCastFrame(obj.value!!)
                }
                INSTANCEOF -> {
                    currentFrame = InstanceOfFrame(stack.pop().value!!)
                    stack.push(StackContext(Int::class, currentFrame))
                }
                MONITORENTER,
                MONITOREXIT -> {
                    currentFrame = MonitorFrame(insn.opcode, stack.pop().value!!)
                }
                MULTIANEWARRAY -> {
                    val cast = insn as MultiNewArrayInstruction
                    val sizes = mutableListOf<Frame>()
                    for(i in 0 until cast.dims) {
                        sizes.add(stack.pop().value!!)
                    }
                    currentFrame = MultiANewArrayFrame(sizes)
                    var desc = cast.desc
                    for(i in 0 until cast.dims) {
                        desc = "[" + desc
                    }
                    stack.push(StackContext(Any::class, currentFrame, desc))
                }
            }

            /*
             * Post opcode processing. We dont care about labels for these checks so Skip opcodes of value -1.
             */
            if(currentFrame != null) {
                result.maxStack = max(result.maxStack, stack.size)
                result.maxLocals = max(result.maxLocals, locals.size)

                val thisFrame = result.frames.computeIfAbsent(insn) { mutableListOf() }
                thisFrame.add(currentFrame)

                /*
                 * Update frame LVT.
                 */
                for(i in locals.indices) {
                    val ctx = locals.getOrNull(i)
                    if(ctx == null) {
                        currentFrame.pushLocal(Value(ValueType.NULL))
                    } else {
                        val type = ctx.valueType
                        val desc = if(type == ValueType.UNINITIALIZED_THIS) method.owner.name else ctx.initType
                        currentFrame.pushLocal(Value(type, desc))
                    }
                }

                /*
                 * Update frame stack.
                 */
                for(i in stack.indices) {
                    val ctx = stack.getOrNull(i) ?: throw IllegalStateException()
                    val type = ctx.valueType
                    val desc = if(type == ValueType.UNINITIALIZED_THIS) method.owner.name else ctx.initType
                    currentFrame.pushStack(Value(type, desc))
                }
            }

            /*
             * Deal with JVM exception handlers. Get the exception handler block the current
             * instruction is in. If the instruction is not apart of a handler, continue without jumping.
             */
            handlers[insn]?.forEach { block ->
                if(jumps.add(AbstractMap.SimpleEntry(insn, block.handler!!))) {
                    val newStack = mutableListOf<StackContext>()
                    newStack.push(StackContext(ArgumentFrame(-1, -1), block.catchType?.name ?: "java/lang/Throwable"))
                    val newLocals = mutableListOf<StackContext?>()
                    newLocals.addAll(locals)
                    /*
                     * Jump and execute the jumped instruction.
                     */
                    execute(method, block.handler!!, newStack, newLocals, handlers, jumps, result)
                }
            }

            /*
             * Exit the loop if the execution is complete.
             */
            if(complete) {
                return
            }

            /*
             * Jump to the next instruction if any exist.
             */
            if(successors.isNotEmpty()) {
                successors.forEach { successor ->
                    if(jumps.add(AbstractMap.SimpleEntry(insn, successor))) {
                        val newStack = mutableListOf<StackContext>()
                        newStack.addAll(stack)
                        val newLocals = mutableListOf<StackContext?>()
                        newLocals.addAll(locals)

                        /*
                         * Execute the successor instruction jump.
                         */
                        execute(method, successor, newStack, newLocals, handlers, jumps, result)
                    }
                }

                /*
                 * Break out of the loop.
                 */
                return
            } else {
                /*
                 * Continue to the next instruction
                 */
                insn = insn.next ?: throw RuntimeException("Execution fall off.")
            }
        }
    }

    /*
     * PRIVATE UTILITY METHODS
     */

    private fun MutableList<StackContext?>.assureSize(size: Int) {
        while(this.size <= size) {
            this.add(null)
        }
    }

    /**
     * Creates and pushes a [StackContext] to a list representing the JVM stack or LVT.
     *
     * @receiver MutableList<StackContext>
     * @param ctx StackContext
     */
    private fun MutableList<StackContext>.push(ctx: StackContext) {
        this.add(0, ctx)
    }

    /**
     * Creates and pushes a 64bit WIDE [StackContext] to a list representing the JVM stack or LVT.
     *
     * @receiver MutableList<StackContext>
     * @param ctx StackContext
     */
    private fun MutableList<StackContext>.pushWide(ctx: StackContext) {
        this.push(ctx)
        this.push(ctx)
    }

    /**
     * Pops a [StackContext] off of a list represents the JVM stack of LVT.
     *
     * @receiver MutableList<StackContext>
     * @return StackContext
     */
    private fun MutableList<StackContext>.pop(): StackContext {
        if(this.isEmpty()) {
            throw RuntimeException("Stack underflow. Tried to pop value off of an empty stack.")
        }

        return this.removeAt(0)
    }

    /**
     * Pops a 64bit WIDE [StackContext] off of a list representing the JVM stack or LVT.
     *
     * @receiver MutableList<StackContext>
     * @return StackContext
     */
    private fun MutableList<StackContext>.popWide(): StackContext {
        if(this.isEmpty() || this.size == 1) {
            throw RuntimeException("Stack underflow. Tried to pop value off of an empty stack.")
        }

        val top = this.pop()
        val bottom = this.pop()

        if(top != bottom) {
            throw RuntimeException("Popped wide values are invalid. Expected '$top' but got '$bottom'.")
        }

        return top
    }
}