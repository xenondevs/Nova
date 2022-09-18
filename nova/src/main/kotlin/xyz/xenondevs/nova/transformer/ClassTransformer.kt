package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import kotlin.reflect.KClass

internal abstract class ClassTransformer(val clazz: KClass<*>, override val computeFrames: Boolean = false): Transformer {
    
    override val classes = setOf(clazz)
    
    protected var classWrapper = VirtualClassPath[clazz]
    
}