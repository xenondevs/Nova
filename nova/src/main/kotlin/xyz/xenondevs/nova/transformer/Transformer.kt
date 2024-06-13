package xyz.xenondevs.nova.transformer

import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.copy
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.util.data.AsmUtils
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

internal sealed interface Transformer {
    
    val classes: Set<KClass<*>>
    
    val computeFrames: Boolean
    
    fun transform()
    
    fun shouldTransform(): Boolean = true
    
    fun KFunction<*>.replaceWith(other: KFunction<*>) {
        javaMethod!!.replaceWith(other.javaMethod!!)
    }
    
    fun Method.replaceWith(other: KFunction<*>) {
        replaceWith(other.javaMethod!!)
    }
    
    fun KFunction<*>.replaceWith(other: Method) {
        javaMethod!!.replaceWith(other)
    }
    
    fun Method.replaceWith(other: Method) {
        val thisNode = VirtualClassPath[this]
        val otherNode = VirtualClassPath[other]
        thisNode.replaceWith(otherNode)
    }
    
    fun MethodNode.replaceWith(other: MethodNode) {
        clear()
        instructions = other.instructions
        tryCatchBlocks = other.tryCatchBlocks
    }
    
    fun MethodNode.replaceInstructions(build: InsnBuilder.() -> Unit) {
        clear()
        instructions = buildInsnList(build)
    }
    
    fun MethodNode.replaceInstructions(insns: InsnList) {
        clear()
        instructions = insns.copy()
    }
    
    fun MethodNode.delegateStatic(other: KFunction<*>) {
        delegateStatic(other.javaMethod!!)
    }
    
    fun MethodNode.delegateStatic(other: Method) {
        delegateStatic(other.declaringClass.internalName, VirtualClassPath[other], other.declaringClass.isInterface)
    }
    
    fun MethodNode.delegateStatic(owner: String, other: MethodNode, isInterface: Boolean) {
        clear()
        instructions = AsmUtils.createDelegateInstructions(
            InsnList(),
            buildInsnList { invokeStatic(owner, other.name, other.desc, isInterface) },
            other,
            0
        )
    }
    
    private fun MethodNode.clear() {
        localVariables?.clear()
        tryCatchBlocks?.clear()
        visibleLocalVariableAnnotations?.clear()
        invisibleLocalVariableAnnotations?.clear()
    }
    
}