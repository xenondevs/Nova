package xyz.xenondevs.nova.patch

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.util.addSuffix
import java.io.File
import kotlin.reflect.KClass

internal abstract class MultiTransformer(final override val classes: Set<KClass<*>>, override val computeFrames: Boolean = true) : Transformer {
    
    constructor(vararg classes: KClass<*>, computeFrames: Boolean = true) : this(classes.toSet(), computeFrames)
    
    protected val classWrappers = classes.associate { it.internalName to VirtualClassPath[it] }
    
    fun dumpAll(computeFrames: Boolean = false) {
        classWrappers.forEach { (_, wrapper) ->
            File(wrapper.className.addSuffix(".class")).writeBytes(wrapper.assemble(computeFrames))
        }
    }
    
}