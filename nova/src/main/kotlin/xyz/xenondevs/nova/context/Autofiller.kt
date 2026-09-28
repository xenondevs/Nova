package xyz.xenondevs.nova.context

/**
 * The scope with which an [Autofiller] is executed. Allows resolving other param types.
 */
interface ContextParamResolver<I : ContextIntention<I>> {
    
    /**
     * Resolves [paramType] through explicit values or autofillers, returning `null` if unavailable.
     * Default values are not available to autofillers.
     */
    fun <V : Any> resolve(paramType: ContextParamType<V, I>): V?
    
}

/**
 * Infers context values from other context values.
 */
fun interface Autofiller<out V, I : ContextIntention<I>> {
    
    /**
     * Fills the value [V] based on the existing values resolvable through [ctx].
     */
    fun fill(ctx: ContextParamResolver<I>): V?
    
    companion object {
        
        /**
         * Creates an [Autofiller] that generates a value of type [V] using [fillValue] and
         * values available through the provided [ContextParamResolver].
         */
        fun <V : Any, I : ContextIntention<I>> dynamic(
            fillValue: ContextParamResolver<I>.() -> V?
        ): Autofiller<V, I> = Autofiller { ctx -> ctx.fillValue() }
        
        /**
         * Creates an [Autofiller] that generates a value of type [V] using [fillValue].
         */
        fun <V : Any, I : ContextIntention<I>> from(
            fillValue: () -> V?
        ): Autofiller<V, I> = Autofiller { fillValue() } 
        
        /**
         * Creates an [Autofiller] that uses [paramTypeA] to generate a value
         * of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            fillValue: (A) -> V?
        ): Autofiller<V, I> = Autofiller { ctx ->
            val a = ctx.resolve(paramTypeA) ?: return@Autofiller null
            fillValue(a)
        }
        /**
         * Creates an [Autofiller] that uses [paramTypeA] and [paramTypeB] to
         * generate a value of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, B : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            fillValue: (A, B) -> V?
        ): Autofiller<V, I> = Autofiller { ctx ->
            val a = ctx.resolve(paramTypeA) ?: return@Autofiller null
            val b = ctx.resolve(paramTypeB) ?: return@Autofiller null
            fillValue(a, b)
        }
        /**
         * Creates an [Autofiller] that uses [paramTypeA], [paramTypeB] and
         * [paramTypeC] to generate a value of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, B : Any, C : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            paramTypeC: ContextParamType<C, I>,
            fillValue: (A, B, C) -> V?
        ): Autofiller<V, I> = Autofiller { ctx ->
            val a = ctx.resolve(paramTypeA) ?: return@Autofiller null
            val b = ctx.resolve(paramTypeB) ?: return@Autofiller null
            val c = ctx.resolve(paramTypeC) ?: return@Autofiller null
            fillValue(a, b, c)
        }
        /**
         * Creates an [Autofiller] that uses [paramTypeA], [paramTypeB], [paramTypeC]
         * and [paramTypeD] to generate a value of type [V] using [fillValue].
         */
        fun <V : Any, A : Any, B : Any, C : Any, D : Any, I : ContextIntention<I>> from(
            paramTypeA: ContextParamType<A, I>,
            paramTypeB: ContextParamType<B, I>,
            paramTypeC: ContextParamType<C, I>,
            paramTypeD: ContextParamType<D, I>,
            fillValue: (A, B, C, D) -> V?
        ): Autofiller<V, I> = Autofiller { ctx ->
            val a = ctx.resolve(paramTypeA) ?: return@Autofiller null
            val b = ctx.resolve(paramTypeB) ?: return@Autofiller null
            val c = ctx.resolve(paramTypeC) ?: return@Autofiller null
            val d = ctx.resolve(paramTypeD) ?: return@Autofiller null
            fillValue(a, b, c, d)
        }
        
    }
    
}
