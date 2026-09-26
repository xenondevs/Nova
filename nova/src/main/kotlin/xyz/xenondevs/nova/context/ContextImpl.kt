package xyz.xenondevs.nova.context

internal class ContextImpl<I : ContextIntention<I>> private constructor(
    override val intention: I,
    private val params: Array<Any?>,
    private val explicitParams: Array<Any?>,
) : Context<I> {
    
    override operator fun <V : Any> get(paramType: ContextParamType<V, I>): V? =
        getParam(paramType)?.let { paramType.copy(it) }
    
    override operator fun <V : Any> get(paramType: DefaultingContextParamType<V, I>): V =
        paramType.copy(getParam(paramType) ?: paramType.default)
    
    override operator fun <V : Any> get(paramType: RequiredContextParamType<V, I>): V {
        val result = requireNotNull(getParam(paramType)) { "$paramType is/was not registered as required in $intention" }
        return paramType.copy(result)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> getParam(paramType: ContextParamType<V, I>): V? =
        params.getOrNull(paramType.id) as V?
    
    override fun toBuilder(): Context.Builder<I> =
        Builder(intention, explicitParams.copyOf(intention.paramTypes.size))
    
    class Builder<I : ContextIntention<I>> internal constructor(
        private val intention: I,
        private val explicitParams: Array<Any?> = arrayOfNulls(intention.paramTypes.size)
    ) : Context.Builder<I> {
        
        override fun <V : Any> param(paramType: ContextParamType<V, I>, value: V?): Builder<I> {
            if (value != null)
                require(paramType.validate(value)) { "Invalid value $value for context parameter ${paramType.id}" }
            explicitParams[paramType.id] = value
            return this
        }
        
        override fun build(): Context<I> {
            return ContextImpl(
                intention = intention,
                params = Session().resolveAndValidate(),
                explicitParams = explicitParams.clone()
            )
        }
        
        private inner class Session : ContextParamResolver<I> {
            
            private val resolvedParams = explicitParams.clone()
            private val inProgress = BooleanArray(resolvedParams.size)
            
            @Suppress("UNCHECKED_CAST")
            override fun <V : Any> resolve(paramType: ContextParamType<V, I>): V? {
                val id = paramType.id
                val existingValue = resolvedParams[id]
                if (existingValue != null)
                    return existingValue as V
                if (inProgress[id])
                    return null
                
                inProgress[id] = true
                try {
                    for (autofiller in intention.getAutofillers(paramType)) {
                        val value = autofiller.fill(this)
                            ?: continue
                        check(paramType.validate(value)) { "Autofiller produced invalid value $value for context parameter $id" }
                        resolvedParams[id] = value
                        return value
                    }
                    return null
                } finally {
                    inProgress[id] = false
                }
            }
            
            fun resolveAndValidate(): Array<Any?> {
                while (true) {
                    val resolvedBefore = resolvedParams.count { it != null }
                    for (paramType in intention.paramTypes)
                        resolve(paramType)
                    if (resolvedParams.count { it != null } == resolvedBefore)
                        break
                }
                
                for (paramType in intention.requiredParamTypes) {
                    if (resolvedParams[paramType.id] == null)
                        throw IllegalStateException("Required context parameter $paramType is not present")
                }
                return resolvedParams
            }
            
        }
        
    }
    
}