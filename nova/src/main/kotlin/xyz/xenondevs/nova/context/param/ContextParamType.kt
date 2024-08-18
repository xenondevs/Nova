@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.context.param

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.context.intention.ContextIntention

class Requirement<V : Any>(
    val validator: (V) -> Boolean,
    val errorGenerator: (V) -> String
)

class Autofiller<V : Any>(
    lazyParamTypes: List<Lazy<ContextParamType<*>>>,
    val filler: Function<V?>
) {
    
    val params: List<ContextParamType<*>> by lazy { lazyParamTypes.map { it.value } }
    
    operator fun component1(): List<ContextParamType<*>> = params
    operator fun component2(): Function<V?> = filler
    
}

/**
 * A context parameter type.
 */
sealed interface ContextParamType<V : Any> {
    
    /**
     * The ID of this parameter type.
     */
    val id: ResourceLocation
    
    /**
     * A list of requirements that must be fulfilled for a value of this parameter type to be valid.
     */
    val requirements: List<Requirement<V>>
    
    val autofillers: List<Autofiller<V>>
    
    val copy: (V) -> V
    
    companion object {
        
        fun <V : Any> builder(addon: Addon, name: String): ContextParamTypeBuilder<V> {
            return builder(ResourceLocation.fromNamespaceAndPath(addon.description.id, name))
        }
        
        internal fun <V : Any> builder(name: String): ContextParamTypeBuilder<V> {
            return builder(ResourceLocation.fromNamespaceAndPath("nova", name))
        }
        
        fun <V : Any> builder(id: ResourceLocation): ContextParamTypeBuilder<V> {
            return ContextParamTypeBuilder(id)
        }
        
    }
    
}

/**
 * A context parameter type that has a default value instead of null.
 */
sealed interface DefaultingContextParamType<V : Any> : ContextParamType<V> {
    
    /**
     * The default intermediate value of this parameter type.
     */
    val defaultValue: V
    
}

internal open class ContextParamTypeImpl<V : Any>(
    override val id: ResourceLocation,
    override val requirements: List<Requirement<V>>,
    override val autofillers: List<Autofiller<V>>,
    override val copy: (V) -> V
) : ContextParamType<V> {
    
    override fun toString() = id.toString()
    
}

internal class DefaultingContextParamTypeImpl<V : Any>(
    id: ResourceLocation,
    override val defaultValue: V,
    requirements: List<Requirement<V>>,
    autofillers: List<Autofiller<V>>,
    copy: (V) -> V
) : ContextParamTypeImpl<V>(id, requirements, autofillers, copy), DefaultingContextParamType<V>

class ContextParamTypeBuilder<V : Any> internal constructor(private val id: ResourceLocation) {
    
    private val requirements = ArrayList<Requirement<V>>()
    private val autofillers = ArrayList<Autofiller<V>>()
    private var copy: (V) -> V = { it }
    
    private val requiredIntentions = HashSet<ContextIntention>()
    private val optionalIntentions = HashSet<ContextIntention>()
    
    fun require(validator: (V) -> Boolean, errorGenerator: (V) -> String): ContextParamTypeBuilder<V> {
        return require(Requirement(validator, errorGenerator))
    }
    
    fun require(requirement: Requirement<V>): ContextParamTypeBuilder<V> {
        requirements += requirement
        return this
    }
    
    fun <A : Any> autofilledBy(
        lazyParamType: () -> ContextParamType<A>,
        fillValue: (A) -> V?
    ) = autofilledBy(fillValue, lazyParamType)
    
    fun <A : Any, B : Any> autofilledBy(
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        fillValue: (A, B) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB)
    
    fun <A : Any, B : Any, C : Any> autofilledBy(
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        lazyParamTypeC: () -> ContextParamType<C>,
        fillValue: (A, B, C) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB, lazyParamTypeC)
    
    fun <A : Any, B : Any, C : Any, D : Any> autofilledBy(
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        lazyParamTypeC: () -> ContextParamType<C>,
        lazyParamTypeD: () -> ContextParamType<D>,
        fillValue: (A, B, C, D) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB, lazyParamTypeC, lazyParamTypeD)
    
    private fun autofilledBy(
        fillValue: Function<V?>,
        vararg lazyParamTypes: () -> ContextParamType<*>
    ): ContextParamTypeBuilder<V> {
        val paramTypes = lazyParamTypes.map(::lazy)
        autofillers += Autofiller(paramTypes, fillValue)
        return this
    }
    
    fun requiredIn(vararg intention: ContextIntention): ContextParamTypeBuilder<V> {
        requiredIntentions += intention
        return this
    }
    
    fun optionalIn(vararg intention: ContextIntention): ContextParamTypeBuilder<V> {
        optionalIntentions += intention
        return this
    }
    
    fun copiedBy(copy: (V) -> V): ContextParamTypeBuilder<V> {
        this.copy = copy
        return this
    }
    
    fun build(): ContextParamType<V> {
        val type = ContextParamTypeImpl(id, requirements, autofillers, copy)
        register(type)
        return type
    }
    
    fun build(default: V): DefaultingContextParamType<V> {
        val type = DefaultingContextParamTypeImpl(id, default, requirements, autofillers, copy)
        register(type)
        return type
    }
    
    private fun register(type: ContextParamType<V>) {
        for (intention in requiredIntentions) {
            intention.addRequired(type)
        }
        for (intention in optionalIntentions) {
            intention.addOptional(type)
        }
    }
    
}