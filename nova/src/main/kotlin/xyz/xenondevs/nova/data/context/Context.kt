@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.context

import xyz.xenondevs.commons.collections.filterValuesNotNullTo
import xyz.xenondevs.commons.reflection.call
import xyz.xenondevs.nova.data.context.intention.ContextIntention
import xyz.xenondevs.nova.data.context.param.ContextParamType
import xyz.xenondevs.nova.data.context.param.DefaultingContextParamType

class Context<I : ContextIntention> private constructor(
    private val intention: I,
    private val explicitParams: Map<ContextParamType<*>, Any>,
    private val resolvedParams: Map<ContextParamType<*>, Any>,
) {
    
    /**
     * Returns the value of the given [paramType] or null if it is not present.
     */
    operator fun <V : Any> get(paramType: ContextParamType<V>): V? {
        return getParam(paramType)
    }
    
    /**
     * Returns the value of the given [paramType].
     */
    operator fun <V : Any> get(paramType: DefaultingContextParamType<V>): V {
        return getParam(paramType) ?: paramType.defaultValue
    }
    
    /**
     * Returns the value of the given [paramType] or throws an exception if it is not present.
     *
     * @throws IllegalStateException If the given [paramType] is an optional parameter that is not present.
     * @throws IllegalArgumentException If the given [paramType] is not allowed under this context's intention.
     */
    fun <V : Any> getOrThrow(paramType: ContextParamType<V>): V {
        val value = getParam(paramType)
        
        if (value != null)
            return value
        
        if (paramType is DefaultingContextParamType)
            return paramType.defaultValue
        
        throwParamNotPresent(paramType)
    }
    
    private fun <V : Any> getParam(paramType: ContextParamType<V>): V? {
        return (explicitParams[paramType] ?: resolvedParams[paramType]) as V?
    }
    
    
    /**
     * Checks whether the given [paramType] is present in this context.
     *
     * @throws IllegalArgumentException When the given [paramType] is not allowed under this context's intention.
     */
    fun has(paramType: ContextParamType<*>): Boolean {
        if (paramType in explicitParams || paramType in resolvedParams)
            return true
        
        if (paramType !in intention.all)
            throw IllegalArgumentException("A context of intention $intention will never contain parameter ${paramType.id}")
        
        return false
    }
    
    /**
     * Checks whether the given [paramType] is explicitly specified in this context.
     *
     * @throws IllegalArgumentException When the given [paramType] is not allowed under this context's intention.
     */
    fun hasExplicitly(paramType: ContextParamType<*>): Boolean {
        if (paramType in explicitParams)
            return true
        
        if (paramType !in intention.all)
            throw IllegalArgumentException("A context of intention $intention will never contain parameter ${paramType.id}")
        
        return false
    }
    
    private fun throwParamNotPresent(paramType: ContextParamType<*>): Nothing {
        if (paramType in intention.all)
            throw IllegalStateException("Context parameter ${paramType.id} is not present")
        else throw IllegalArgumentException("Context parameter ${paramType.id} is not allowed")
    }
    
    companion object {
        
        /**
         * Creates a new context builder for the given [intention].
         */
        fun <I : ContextIntention> intention(intention: I): Builder<I> {
            return Builder(intention)
        }
        
    }
    
    class Builder<I : ContextIntention> internal constructor(private val intention: I) {
        
        /**
         * The parameters that are explicitly set.
         */
        private val explicitParams = HashMap<ContextParamType<*>, Any>()
        
        /**
         * The parameters that are loaded through autofillers. The value is null if the param could not be loaded
         * through fallbacks.
         */
        private val resolvedParams = HashMap<ContextParamType<*>, Any?>()
        
        /**
         * Sets the given [paramType] to the given [value].
         */
        fun <V : Any> param(paramType: ContextParamType<V>, value: V?): Builder<I> {
            if (paramType !in intention.all)
                throw IllegalArgumentException("Context parameter ${paramType.id} is not allowed under intention $intention")
            
            if (value == null) {
                explicitParams.remove(paramType)
            } else {
                // check requirements
                for (requirement in paramType.requirements) {
                    if (!requirement.validator(value))
                        throw IllegalArgumentException("Context value: $value for parameter type: ${paramType.id} is invalid: ${requirement.errorGenerator(value)}")
                }
                
                explicitParams[paramType] = value
            }
            
            return this
        }
        
        /**
         * Builds the context.
         */
        fun build(autofill: Boolean = true): Context<I> {
            if (autofill)
                resolveParams()
            
            // verify presence of all required params
            for (requiredParam in intention.required) {
                if (requiredParam !in explicitParams)
                    throw IllegalStateException("Required context parameter ${requiredParam.id} is not present")
            }
            
            return Context(
                intention,
                HashMap(explicitParams),
                resolvedParams.filterValuesNotNullTo(HashMap())
            )
        }
        
        private fun resolveParams() {
            for (paramType in intention.all) {
                resolveParam(paramType)
            }
        }
        
        private fun hasParam(paramType: ContextParamType<*>): Boolean =
            paramType in explicitParams || paramType in resolvedParams
        
        private fun <V : Any> getParam(paramType: ContextParamType<V>): V? =
            explicitParams[paramType] as V? ?: resolvedParams[paramType] as V?
        
        private fun <V : Any> resolveParam(paramType: ContextParamType<V>): V? {
            if (hasParam(paramType))
                return getParam(paramType)
            
            // preemptively set this to null to prevent recursive call chains
            resolvedParams[paramType] = null
            
            // try to resolve value through autofillers
            val autofillers = paramType.autofillers
            var value: V? = null
            autofiller@ for ((requiredParamTypes, fillerFunction) in autofillers) {
                // load params required by autofiller
                val requiredParamValues = arrayOfNulls<Any>(requiredParamTypes.size)
                for ((i, requiredParamType) in requiredParamTypes.withIndex()) {
                    val requiredParamValue = resolveParam(requiredParamType)
                        ?: continue@autofiller // try next autofiller
                    requiredParamValues[i] = requiredParamValue
                }
                
                // run autofiller function
                value = fillerFunction.call(*requiredParamValues)
                
                if (value != null && paramType.requirements.all { it.validator(value!!) })
                    break
            }
            
            // otherwise, use default value if present
            if (value == null && paramType is DefaultingContextParamType)
                value = paramType.defaultValue
            
            resolvedParams[paramType] = value
            return value
        }
        
    }
    
}
