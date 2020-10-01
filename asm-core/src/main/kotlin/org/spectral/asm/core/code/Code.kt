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

package org.spectral.asm.core.code

import org.objectweb.asm.MethodVisitor
import org.spectral.asm.core.Method
import org.spectral.asm.core.code.type.*
import org.spectral.asm.core.reference.ClassRef
import java.util.*
import org.objectweb.asm.Label as AsmLabel

/**
 * Represents the instructions contained within a java method.
 *
 * @property method Method
 * @constructor
 */
class Code(val method: Method) {

    /**
     * The backing storage of instruction objects contained within this object.
     */
    val instructions = LinkedList<Instruction>()

    /**
     * The maximum size of the stack for the associated [method].
     */
    var maxStack = 0

    /**
     * The maximum size of the LVT for the associated [method].
     */
    var maxLocals = 0

    /**
     *  A list of try-catch exception blocks.
     */
    val exceptions = mutableListOf<Exception>()

    /**
     *  A map of ASM [AsmLabel] to [Label] mappings used for tracking where label blocks point to.
     */
    private val labelMap = hashMapOf<AsmLabel, Label>()

    /**
     * Makes the given [visitor] visit all the instruction related values in this object.
     *
     * @param visitor MethodVisitor
     */
    fun accept(visitor: MethodVisitor) {
        visitor.visitCode()

        exceptions.forEach { exception ->
            visitor.visitTryCatchBlock(exception.start.label, exception.end.label, exception.handler?.label, exception.catchType?.name)
        }

        instructions.forEach { insn ->
            insn.accept(visitor)
        }

        visitor.visitMaxs(maxStack, maxLocals)
    }

    /**
     * Initializes all the references of this object.
     */
    internal fun init() {
        /*
         * Set all the instructions [Code] reference to this object.
         */
        instructions.forEach { insn ->
            insn.code = this
            insn.init()
        }
    }

    /**
     * Finds a [Label] mapped to an ASM [AsmLabel] object. If none is found,
     * a new mapping is created.
     *
     * @param label Label
     * @return Label
     */
    internal fun getOrCreateLabel(label: AsmLabel): Label {
        return labelMap[label] ?: Label(label).apply {
            this.id = labelMap.size + 1
            labelMap[this.label] = this
        }
    }

    /*
     * VISITOR METHOD
     * Delegated from the [Method] object's visitor methods.
     */

    internal fun visitTryCatchBlock(start: AsmLabel, end: AsmLabel, handler: AsmLabel?, catchType: String?) {
        val exception = Exception(this, getOrCreateLabel(start), getOrCreateLabel(end), handler?.let { getOrCreateLabel(it) }, catchType?.let { ClassRef(it) })
        this.exceptions.add(exception)
    }

    internal fun visitLabel(label: AsmLabel) {
        /*
         * Create a new label.
         */
        getOrCreateLabel(label).apply { instructions.add(this) }
    }

    internal fun visitLineNumber(line: Int, start: AsmLabel) {
        /*
         * Create a new linenumber instruction.
         */
        LineNumber(line, getOrCreateLabel(start)).apply { instructions.add(this) }
    }

    internal fun visitInsn(opcode: Int) {
        this.instructions.add(Instruction(opcode))
    }

    internal fun visitLdcInsn(value: Any) {
        this.instructions.add(LdcInstruction(value))
    }

    internal fun visitIincInsn(index: Int, inc: Int) {
        this.instructions.add(IncInstruction(index, inc))
    }

    internal fun visitIntInsn(opcode: Int, operand: Int) {
        this.instructions.add(IntInstruction(opcode, operand))
    }

    internal fun visitVarInsn(opcode: Int, index: Int) {
        this.instructions.add(LVTInstruction(opcode, index))
    }

    internal fun visitJumpInsn(opcode: Int, label: AsmLabel) {
        this.instructions.add(JumpInstruction(opcode, getOrCreateLabel(label)))
    }

    internal fun visitTypeInsn(opcode: Int, type: String) {
        this.instructions.add(TypeInstruction(opcode, ClassRef(type)))
    }

    internal fun visitMaxs(maxStack: Int, maxLocals: Int) {
        this.maxStack = maxStack
        this.maxLocals = maxLocals
    }

}