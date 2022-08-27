package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import kotlin.reflect.KClass

internal abstract class ClassTransformer(val clazz: KClass<*>, val computeFrames: Boolean = false) {
    
    protected var classWrapper = VirtualClassPath[clazz]
    
    abstract fun transform()
    
    open fun shouldTransform(): Boolean = true
    
}