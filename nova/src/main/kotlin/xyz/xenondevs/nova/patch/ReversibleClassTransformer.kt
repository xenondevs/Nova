package xyz.xenondevs.nova.patch

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import kotlin.reflect.KClass

internal abstract class ReversibleClassTransformer(val clazz: KClass<*>, override val computeFrames: Boolean = true): Transformer {
    
    override val classes = setOf(clazz)
    
    protected var classWrapper = VirtualClassPath[clazz]
    
    val initialBytecode: ByteArray = (ClassLoader.getSystemResourceAsStream("${clazz.internalName}.class")
        ?: clazz.java.classLoader.getResourceAsStream("${clazz.internalName}.class")).readAllBytes()
    
}