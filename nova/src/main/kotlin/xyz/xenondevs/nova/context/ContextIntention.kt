package xyz.xenondevs.nova.context

/**
 * Represents an intention for what a context is used for.
 */
interface ContextIntention<I : ContextIntention<I>> {
    
    /**
     * The parameter types that must be present in a context with this intention.
     */
    val required: Set<RequiredContextParamType<*, I>>
    
    fun require(paramType: RequiredContextParamType<*, I>)
    
    fun <V : Any> addAutofiller(
        paramType: ContextParamType<V, I>,
        autofiller: Autofiller<V, I>,
        at: Int = Int.MAX_VALUE
    )
    
    fun <V : Any> getAutofillers(paramType: ContextParamType<V, I>): List<Autofiller<V, I>>
    
}

