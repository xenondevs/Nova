package xyz.xenondevs.nova.context

/**
 * Represents an intention for what a context is used for.
 */
interface ContextIntention<I : ContextIntention<I>> {
    
    /**
     * The parameter types that must be present in a context with this intention.
     */
    val required: Set<RequiredContextParamType<*, I>>
    
    /**
     * Adds [paramType] as a required context parameter type for this intention,
     * forcing all contexts with this intention to have a parameter of this type,
     * either through explicit specification or through autofilling.
     */
    fun require(paramType: RequiredContextParamType<*, I>)
    
    /**
     * Adds an [autofiller] for [paramType] to this intention, 
     * allowing contexts with this intention to autofill parameters
     * of this type if they are not explicitly specified.
     */
    fun <V : Any> addAutofiller(
        paramType: ContextParamType<V, I>,
        autofiller: Autofiller<V, I>,
        at: Int = Int.MAX_VALUE
    )
    
    /**
     * Gets the autofillers for [paramType] in this intention in the order they should be queried.
     */
    fun <V : Any> getAutofillers(paramType: ContextParamType<V, I>): List<Autofiller<V, I>>
    
}

