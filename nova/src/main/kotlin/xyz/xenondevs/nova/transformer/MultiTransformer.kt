package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import kotlin.reflect.KClass

internal abstract class MultiTransformer(override val classes: Set<KClass<*>>, override val computeFrames: Boolean): Transformer {
    
    protected val classWrappers = classes.associate { it.java.name to VirtualClassPath[it] }
    
}