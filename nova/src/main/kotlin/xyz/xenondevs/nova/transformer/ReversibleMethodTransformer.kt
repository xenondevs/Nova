package xyz.xenondevs.nova.transformer

import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Method
import kotlin.reflect.KClass

internal abstract class ReversibleMethodTransformer : ReversibleClassTransformer {
    
    protected val methodNode: MethodNode
    
    constructor(clazz: KClass<*>, method: MethodNode, computeFrames: Boolean = false) : super(clazz, computeFrames) {
        methodNode = method
    }
    
    constructor(clazz: KClass<*>, methodName: String, computeFrames: Boolean = false) : super(clazz, computeFrames) {
        methodNode = classWrapper.getMethod(methodName)!!
    }
    
    constructor(clazz: KClass<*>, methodName: String, desc: String, computeFrames: Boolean = false) : super(clazz, computeFrames) {
        methodNode = classWrapper.getMethod(methodName, desc)!!
    }
    
    constructor(method: Method, computeFrames: Boolean = false) : super(method.declaringClass.kotlin, computeFrames) {
        methodNode = classWrapper.getMethod(method.name, Type.getMethodDescriptor(method))!!
    }
    
}