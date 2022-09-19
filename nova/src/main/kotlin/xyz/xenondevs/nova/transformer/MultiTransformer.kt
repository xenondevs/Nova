package xyz.xenondevs.nova.transformer

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import kotlin.reflect.KClass

internal abstract class MultiTransformer(final override val classes: Set<KClass<*>>, override val computeFrames: Boolean): Transformer {
    
    protected val classWrappers = classes.associate { it.internalName to VirtualClassPath[it] }
    
}