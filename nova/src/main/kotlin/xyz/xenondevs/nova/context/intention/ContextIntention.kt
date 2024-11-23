package xyz.xenondevs.nova.context.intention

import com.google.common.collect.Sets
import xyz.xenondevs.nova.context.param.ContextParamType

/**
 * Represents an intention for what a context is used for.
 */
abstract class ContextIntention {
    
    private val _optional = HashSet<ContextParamType<*>>()
    
    abstract val required: Set<ContextParamType<*>>
    val optional: Set<ContextParamType<*>> get() = _optional
    val all: Set<ContextParamType<*>> get() = Sets.union(required, optional)
    
    fun addOptional(paramType: ContextParamType<*>) {
        _optional.add(paramType)
    }
    
}