package xyz.xenondevs.nova.context

/**
 * Represents an intention for what a context is used for.
 */
interface ContextIntention<I : ContextIntention<I>> {
    
    /**
     * The parameter types that must be present in a context with this intention.
     */
    val requiredParamTypes: List<RequiredContextParamType<*, I>>
    
    /**
     * All parameter types that can be present in a context with this intention,
     * including required, defaulting and optional parameter types.
     */
    val paramTypes: List<ContextParamType<*, I>>
    
    /**
     * Creates a new [RequiredContextParamType] with [validate] and [copy] and registers it with this intention.
     */
    fun <V : Any> addRequiredParamType(
        validate: (V) -> Boolean = { true },
        copy: (V) -> V = { it }
    ): RequiredContextParamType<V, I>
    
    /**
     * Creates a new [DefaultingContextParamType] with [default], [validate] and [copy] and registers it with this intention.
     */
    fun <V : Any> addDefaultingParamType(
        default: V,
        validate: (V) -> Boolean = { true },
        copy: (V) -> V = { it }
    ): DefaultingContextParamType<V, I>
    
    /**
     * Creates a new [ContextParamType] with [validate] and [copy] and registers it with this intention.
     */
    fun <V : Any> addOptionalParamType(
        validate: (V) -> Boolean = { true },
        copy: (V) -> V = { it }
    ): ContextParamType<V, I>
    
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

