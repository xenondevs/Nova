package xyz.xenondevs.nova.context

import xyz.xenondevs.commons.collections.removeIf

internal class ContextImpl<I : ContextIntention<I>> private constructor(
    override val intention: I,
    private val explicitParams: Map<ContextParamType<*, I>, Any>,
    private val resolvedParams: MutableMap<ContextParamType<*, I>, Any?> = HashMap(),
) : Context<I> {
    
    override operator fun <V : Any> get(paramType: ContextParamType<V, I>): V? =
        getParam(paramType)?.let { paramType.copy(it) }
    
    override operator fun <V : Any> get(paramType: DefaultingContextParamType<V, I>): V =
        paramType.copy(getParam(paramType) ?: paramType.default)
    
    override operator fun <V : Any> get(paramType: RequiredContextParamType<V, I>): V {
        val result = getParam(paramType)?.let { paramType.copy(it) }
        if (result == null) {
            require(paramType in intention.required) { "$paramType is not registered as required in $intention" }
            throw AssertionError() // otherwise, presence should be verified on .build()
        }
        return result
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> getParam(paramType: ContextParamType<V, I>): V? {
        if (paramType in explicitParams)
            return explicitParams[paramType] as V?
        
        if (paramType in resolvedParams)
            return resolvedParams[paramType] as V?
        
        return resolveParam(paramType)
    }
    
    private fun <V : Any> resolveParam(paramType: ContextParamType<V, I>): V? {
        if (paramType in explicitParams)
            throw IllegalStateException("Context parameter $paramType is already set")
        if (paramType in resolvedParams)
            throw IllegalStateException("Context parameter $paramType is already resolved")
        
        // preemptively set this to null to prevent recursive call chains
        resolvedParams[paramType] = null
        
        // try to resolve value through autofillers
        val autofillers = intention.getAutofillers(paramType)
        var value: V? = null
        for (autofiller in autofillers) {
            val requiredParamTypes = autofiller.requiredParamTypes
            
            // load params required by autofiller
            val requiredParamValues = Array(requiredParamTypes.size) {
                getParam(requiredParamTypes[it])
                    ?: continue
            }
            
            // run autofiller function
            value = autofiller.fill(requiredParamValues)
                ?: continue
            
            paramType.validate(value)
            break
        }
        
        // otherwise, use default value if present
        if (value == null && paramType is DefaultingContextParamType)
            value = paramType.default
        
        resolvedParams[paramType] = value
        
        // remove all remembered resolve failures as they may succeed now that a new value is available
        if (value != null)
            resolvedParams.removeIf { it.value == null }
        
        return value
    }
    
    override fun toBuilder(): Context.Builder<I> {
        return Builder(intention, HashMap(explicitParams))
    }
    
    class Builder<I : ContextIntention<I>> internal constructor(
        private val intention: I,
        private val explicitParams: MutableMap<ContextParamType<*, I>, Any> = HashMap(),
        private val resolvedParams: MutableMap<ContextParamType<*, I>, Any?> = HashMap()
    ) : Context.Builder<I> {
        
        override fun <V : Any> param(paramType: ContextParamType<V, I>, value: V?): Builder<I> {
            if (value == null) {
                explicitParams.remove(paramType)
            } else {
                paramType.validate(value)
                explicitParams[paramType] = paramType.copy(value)
            }
            
            return this
        }
        
        override fun build(): Context<I> {
            val context = ContextImpl(intention, HashMap(explicitParams), HashMap(resolvedParams))
            
            // verify presence of all required params
            for (requiredParam in intention.required) {
                if (context.getParam(requiredParam) == null)
                    throw IllegalStateException("Required context parameter $requiredParam is not present")
            }
            
            return context
        }
        
    }
    
}
