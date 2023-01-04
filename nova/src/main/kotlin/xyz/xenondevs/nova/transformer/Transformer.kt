package xyz.xenondevs.nova.transformer

import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import java.lang.reflect.Method
import kotlin.reflect.KClass

internal sealed interface Transformer {
    
    val classes: Set<KClass<*>>
    
    val computeFrames: Boolean
    
    fun transform()
    
    fun shouldTransform(): Boolean = true
    
    fun Method.replaceWith(other: Method) {
        val thisNode = VirtualClassPath[this]
        val otherNode = VirtualClassPath[other]
        thisNode.replaceWith(otherNode)
    }
    
    fun MethodNode.replaceWith(other: MethodNode) {
        instructions = other.instructions
        tryCatchBlocks = other.tryCatchBlocks
        localVariables?.clear()
        visibleLocalVariableAnnotations?.clear()
        invisibleLocalVariableAnnotations?.clear()
    }
    
}