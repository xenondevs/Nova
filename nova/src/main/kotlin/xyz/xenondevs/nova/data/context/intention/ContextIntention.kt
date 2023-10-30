package xyz.xenondevs.nova.data.context.intention

import xyz.xenondevs.nova.data.context.param.ContextParamType

/**
 * Represents an intention for what a context is used for.
 * 
 * @param required The required parameters for this intention.
 * @param optional The optional parameters for this intention.
 * @param all All parameters for this intention.
 */
abstract class ContextIntention(
    val required: Set<ContextParamType<*>>,
    val optional: Set<ContextParamType<*>>,
    val all: Set<ContextParamType<*>>
) {
    
    /**
     * Creates an intention with the given [required] and [optional] parameters.
     */
    constructor(required: Collection<ContextParamType<*>>, optional: Collection<ContextParamType<*>>) :
        this(required.toHashSet(), optional.toHashSet(), (required + optional).toHashSet())
    
}