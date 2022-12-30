package xyz.xenondevs.nova.transformer

import kotlin.reflect.KClass

internal sealed interface Transformer {
    
    val classes: Set<KClass<*>>
    
    val computeFrames: Boolean
    
    fun transform()
    
    fun shouldTransform(): Boolean = true
    
}