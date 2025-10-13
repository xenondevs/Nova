package xyz.xenondevs.nova.context

import xyz.xenondevs.commons.reflection.call

/**
 * Infers context values from other context values.
 */
interface Autofiller<out V, I : ContextIntention<I>> {
    
    /**
     * The context parameters types required by this autofiller to create a value of type [V].
     */
    val requiredParamTypes: List<ContextParamType<*, I>>
    
    /**
     * Generates a value of type [V] based on [values], which is expected to be an
     * array with the same length as [requiredParamTypes], where each entry corresponds to the
     * value of the parameter type at the same index in [requiredParamTypes].
     */
    fun fill(values: Array<Any>): V?
    
    companion object {
        
        /**
         * Creates an [Autofiller] that uses [paramTypeA] to generate a value
         * of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            fillValue: (A) -> V?
        ): Autofiller<V, I> = from(listOf(paramTypeA), fillValue)
        
        /**
         * Creates an [Autofiller] that uses [paramTypeA] and [paramTypeB] to
         * generate a value of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, B : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            fillValue: (A, B) -> V?
        ): Autofiller<V, I> = from(listOf(paramTypeA, paramTypeB), fillValue)
        
        /**
         * Creates an [Autofiller] that uses [paramTypeA], [paramTypeB] and
         * [paramTypeC] to generate a value of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, B : Any, C : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            paramTypeC: ContextParamType<C, I>,
            fillValue: (A, B, C) -> V?
        ): Autofiller<V, I> = from(listOf(paramTypeA, paramTypeB, paramTypeC), fillValue)
        
        /**
         * Creates an [Autofiller] that uses [paramTypeA], [paramTypeB], [paramTypeC]
         * and [paramTypeD] to generate a value
         */
        fun <V : Any, A : Any, B : Any, C : Any, D : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            paramTypeC: ContextParamType<C, I>,
            paramTypeD: ContextParamType<D, I>,
            fillValue: (A, B, C, D) -> V?
        ): Autofiller<V, I> = from(listOf(paramTypeA, paramTypeB, paramTypeC, paramTypeD), fillValue)
        
        private fun <V : Any, I : ContextIntention<I>> from(
            paramTypes: List<ContextParamType<*, I>>,
            fillValue: Function<V?>,
            vararg lazyParamTypes: () -> ContextParamType<*, I>
        ) = object : Autofiller<V, I> {
            override val requiredParamTypes = paramTypes
            override fun fill(values: Array<Any>): V? = fillValue.call(*values)
            override fun toString() = "Autofiller(${requiredParamTypes.joinToString()})"
        }
        
    }
    
}

