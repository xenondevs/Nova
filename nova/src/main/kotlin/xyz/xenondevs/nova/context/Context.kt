package xyz.xenondevs.nova.context

import xyz.xenondevs.nova.context.Context.Companion.intention

/**
 * A context maps [ContextParamTypes][ContextParamType] to a values.
 * Each context has an [intention] that defines which parameters are allowed and required.
 *
 * @see Context.Companion.intention
 */
interface Context<I : ContextIntention<I>> {
    
    /**
     * The intention of this context.
     */
    val intention: I
    
    /**
     * Returns the value of the given [paramType] or null if it is not present
     * and could not be resolved through autofillers.
     */
    operator fun <V : Any> get(paramType: ContextParamType<V, I>): V?
    
    /**
     * Returns the value of the given [paramType], falling back to the default value
     * if the param type is not present and could not be resolved through autofillers.
     */
    operator fun <V : Any> get(paramType: DefaultingContextParamType<V, I>): V
    
    /**
     * Returns the value of the given [paramType].
     * 
     * @throws IllegalArgumentException If [paramType] is not a [ContextIntention.required] param type of [intention].
     */
    operator fun <V : Any> get(paramType: RequiredContextParamType<V, I>): V
    
    /**
     * Creates a context builder that is initialized with the explicitly defined parameters of this context.
     */
    fun toBuilder(): Builder<I>
    
    /**
     * Builder for [Context].
     */
    interface Builder<I : ContextIntention<I>> {
        
        /**
         * Sets the given [paramType] to the given [value].
         *
         * @throws IllegalArgumentException If the given [value] is invalid for the given [paramType].
         */
        fun <V : Any> param(paramType: ContextParamType<V, I>, value: V?): Builder<I>
        
        /**
         * Builds the context.
         *
         * @throws IllegalStateException If a required parameter is not present.
         */
        fun build(): Context<I>
        
    }
    
    companion object {
        
        /**
         * Creates a new context builder for the given [intention].
         */
        fun <I : ContextIntention<I>> intention(intention: I): Builder<I> =
            ContextImpl.Builder(intention)
        
    }
    
}