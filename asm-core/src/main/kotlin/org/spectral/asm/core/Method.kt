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

package org.spectral.asm.core

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.spectral.asm.core.code.Code
import java.lang.reflect.Modifier
import org.objectweb.asm.Label as AsmLabel

/**
 * Represents a Java method which is contained within a class.
 *
 * @property owner Class
 * @constructor
 */
class Method(val owner: Class) : MethodVisitor(ASM9) {

    /**
     * The bit-packed visibility acces flags of the method.
     */
    var access = 0

    /**
     * The name of the method.
     */
    lateinit var name: String

    /**
     * The descriptor of the method
     */
    val desc: String get() = Type.getMethodDescriptor(this.returnType, *this.argumentTypes.toTypedArray())

    /**
     * A list of argument ASM [Type]s.
     */
    val argumentTypes = mutableListOf<Type>()

    /**
     * The return ASM [Type] of this method.
     */
    lateinit var returnType: Type

    /**
     * Creates a new method with initialized visitor values.
     *
     * @param owner Class
     * @param access Int
     * @param name String
     * @param desc String
     * @constructor
     */
    constructor(owner: Class, access: Int, name: String, desc: String) : this(owner) {
        this.access = access
        this.name = name

        val type = Type.getMethodType(desc)

        this.argumentTypes.addAll(type.argumentTypes)
        this.returnType = type.returnType
    }

    /**
     * The code object holding instruction related data for this method.
     */
    val code = Code(this)

    /**
     * Whether the method is a static method.
     */
    val isStatic: Boolean get() = Modifier.isStatic(access)

    /**
     * Whether the field is a private field.
     */
    val isPrivate: Boolean get() = Modifier.isPrivate(access)

    /**
     * Initializes the method refs.
     */
    internal fun init() {
        /*
         * Initialize the code instance.
         */
        code.init()
    }

    /*
     * VISITOR METHODS
     */

    override fun visitCode() {
        /*
         * Nothing to do.
         */
    }

    override fun visitTryCatchBlock(start: AsmLabel, end: AsmLabel, handler: AsmLabel?, type: String?)  = code.visitTryCatchBlock(start, end, handler, type)
    override fun visitLabel(label: AsmLabel) = code.visitLabel(label)
    override fun visitLineNumber(line: Int, start: AsmLabel) = code.visitLineNumber(line, start)
    override fun visitInsn(opcode: Int) = code.visitInsn(opcode)
    override fun visitLdcInsn(value: Any) = code.visitLdcInsn(value)
    override fun visitIincInsn(index: Int, increment: Int) = code.visitIincInsn(index, increment)
    override fun visitIntInsn(opcode: Int, operand: Int) = code.visitIntInsn(opcode, operand)
    override fun visitMaxs(maxStack: Int, maxLocals: Int) = code.visitMaxs(maxStack, maxLocals)

    override fun visitEnd() {
        /*
         * Nothing to do.
         */
    }

    fun accept(visitor: MethodVisitor) {
        code.accept(visitor)
        visitor.visitEnd()
    }

    override fun toString(): String {
        return "$owner.$name$desc"
    }
}