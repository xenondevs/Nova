@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.context

import xyz.xenondevs.commons.reflection.call
import xyz.xenondevs.nova.context.intention.ContextIntention
import xyz.xenondevs.nova.context.param.ContextParamType
import xyz.xenondevs.nova.context.param.DefaultingContextParamType

/**
 * A context contains mappings from [ContextParamType] to value.
 * Each context has an [intention] that defines which parameters are allowed and required.
 *
 * With [Context.intention], you can create a new context builder for the given intention.
 *
 * @param I The intention of this context.
 * @param intention The intention of this context.
 * @param explicitParams The parameters that are explicitly set.
 * @param resolvedParams The parameters that are loaded through autofillers. The value is null if the param could not be loaded through autofillers.
 */
class Context<I : ContextIntention> private constructor(
    private val intention: I,
    private val explicitParams: Map<ContextParamType<*>, Any>,
    private val resolvedParams: MutableMap<ContextParamType<*>, Any?> = HashMap(),
) {
    
    /**
     * Returns the value of the given [paramType] or null if it is not present
     * and could not be resolved through autofillers.
     */
    operator fun <V : Any> get(paramType: ContextParamType<V>): V? =
        getParam(paramType)
    
    /**
     * Returns the value of the given [paramType], falling back to the default value
     * if the param type is not present and could not be resolved through autofillers.
     */
    operator fun <V : Any> get(paramType: DefaultingContextParamType<V>): V =
        paramType.copy(getParam(paramType) ?: paramType.defaultValue)
    
    /**
     * Returns the value of the given [paramType] or throws an exception if it is not present
     * and also couldn't be resolved through autofillers.
     *
     * @throws IllegalStateException If the given [paramType] is an optional parameter that is not present.
     * @throws IllegalArgumentException If the given [paramType] is not allowed under this context's intention.
     */
    fun <V : Any> getOrThrow(paramType: ContextParamType<V>): V {
        val value = getParam(paramType)
        
        if (value != null)
            return paramType.copy(value)
        
        if (paramType is DefaultingContextParamType)
            return paramType.copy(paramType.defaultValue)
        
        throwParamNotPresent(paramType)
    }
    
    private fun <V : Any> getParam(paramType: ContextParamType<V>): V? {
        if (paramType in explicitParams)
            return explicitParams[paramType] as V?
        
        if (paramType in resolvedParams)
            return resolvedParams[paramType] as V?
        
        return resolveParam(paramType)
    }
    
    private fun <V : Any> resolveParam(paramType: ContextParamType<V>): V? {
        if (paramType in explicitParams)
            throw IllegalStateException("Context parameter ${paramType.id} is already set")
        if (paramType in resolvedParams)
            throw IllegalStateException("Context parameter ${paramType.id} is already resolved")
        
        // preemptively set this to null to prevent recursive call chains
        resolvedParams[paramType] = null
        
        // try to resolve value through autofillers
        val autofillers = paramType.autofillers
        var value: V? = null
        autofiller@ for ((requiredParamTypes, fillerFunction) in autofillers) {
            // load params required by autofiller
            val requiredParamValues = arrayOfNulls<Any>(requiredParamTypes.size)
            for ((i, requiredParamType) in requiredParamTypes.withIndex()) {
                val requiredParamValue = getParam(requiredParamType)
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
    
    private fun throwParamNotPresent(paramType: ContextParamType<*>): Nothing {
        if (paramType in intention.all)
            throw IllegalStateException("Context parameter ${paramType.id} is not present")
        else throw IllegalArgumentException("Context parameter ${paramType.id} is not allowed")
    }
    
    companion object {
        
        /**
         * Creates a new context builder for the given [intention].
         */
        fun <I : ContextIntention> intention(intention: I): Builder<I> =
            Builder(intention)
        
        /**
         * Creates a new context builder filled with the parameters of the given [context].
         */
        fun <I : ContextIntention> from(context: Context<I>): Builder<I> =
            Builder(context.intention, HashMap(context.explicitParams), HashMap(context.resolvedParams))
        
    }
    
    /**
     * Builder for [Context].
     */
    class Builder<I : ContextIntention> internal constructor(
        private val intention: I,
        private val explicitParams: MutableMap<ContextParamType<*>, Any> = HashMap(),
        private val resolvedParams: MutableMap<ContextParamType<*>, Any?> = HashMap()
    ) {
        
        /**
         * Sets the given [paramType] to the given [value].
         *
         * @throws IllegalArgumentException If the given [paramType] is not allowed under this context's intention.
         * @throws IllegalArgumentException If the given [value] is invalid for the given [paramType].
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
                
                explicitParams[paramType] = paramType.copy(value)
            }
            
            return this
        }
        
        /**
         * Builds the context.
         *
         * @throws IllegalStateException If a required parameter is not present.
         */
        fun build(): Context<I> {
            val context = Context(intention, HashMap(explicitParams), HashMap(resolvedParams))
            
            // verify presence of all required params
            for (requiredParam in intention.required) {
                if (context[requiredParam] == null)
                    throw IllegalStateException("Required context parameter ${requiredParam.id} is not present")
            }
            
            return context
        }
        
    }
    
}
