package xyz.xenondevs.nova.data.context.intention

import xyz.xenondevs.nova.data.context.param.ContextParamType

/**
 * Represents an intention for what a context is used for.
 */
abstract class ContextIntention {
    
    private val _required = HashSet<ContextParamType<*>>()
    private val _optional = HashSet<ContextParamType<*>>()
    private val _all = HashSet<ContextParamType<*>>()
    
    val required: Set<ContextParamType<*>> get() = _required
    val optional: Set<ContextParamType<*>> get() = _optional
    val all: Set<ContextParamType<*>> get() = _all
    
    fun addRequired(paramType: ContextParamType<*>) {
        _required.add(paramType)
        _all.add(paramType)
    }
    
    fun addOptional(paramType: ContextParamType<*>) {
        _optional.add(paramType)
        _all.add(paramType)
    }
    
}