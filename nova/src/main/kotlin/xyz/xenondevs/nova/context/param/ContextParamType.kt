@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.context.param

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.context.intention.ContextIntention

internal class Requirement<V : Any>(
    val validator: (V) -> Boolean,
    val errorGenerator: (V) -> String
)

internal class Autofiller<V : Any>(
    val intention: ContextIntention? = null,
    lazyParamTypes: List<Lazy<ContextParamType<*>>>,
    val filler: Function<V?>
) {
    
    val params: List<ContextParamType<*>> by lazy { lazyParamTypes.map { it.value } }
    
    operator fun component1(): ContextIntention? = intention
    operator fun component2(): List<ContextParamType<*>> = params
    operator fun component3(): Function<V?> = filler
    
}

/**
 * A context parameter type.
 */
sealed class ContextParamType<V : Any> {
    
    /**
     * The ID of this parameter type.
     */
    abstract val id: Key
    
    /**
     * A list of requirements that must be fulfilled for a value of this parameter type to be valid.
     */
    internal abstract val requirements: List<Requirement<V>>
    
    /**
     * A list of autofillers that can create a value of this parameter type based on other parameters.
     */
    internal abstract val autofillers: List<Autofiller<V>>
    
    /**
     * A function that creates a copy of a value of this parameter type.
     */
    internal abstract val copy: (V) -> V
    
    override fun toString() = id.toString()
    
    companion object {
        
        fun <V : Any> builder(addon: Addon, name: String): ContextParamTypeBuilder<V> {
            return builder(Key.key(addon.id, name))
        }
        
        internal fun <V : Any> builder(name: String): ContextParamTypeBuilder<V> {
            return builder(Key.key("nova", name))
        }
        
        fun <V : Any> builder(id: Key): ContextParamTypeBuilder<V> {
            return ContextParamTypeBuilder(id)
        }
        
    }
    
}

/**
 * A context parameter type that has a default value instead of null.
 */
sealed class DefaultingContextParamType<V : Any> : ContextParamType<V>() {
    
    /**
     * The default intermediate value of this parameter type.
     */
    abstract val defaultValue: V
    
}

internal open class ContextParamTypeImpl<V : Any>(
    override val id: Key,
    override val requirements: List<Requirement<V>>,
    override val autofillers: List<Autofiller<V>>,
    override val copy: (V) -> V
) : ContextParamType<V>() {
    
    override fun toString() = id.toString()
    
}

internal class DefaultingContextParamTypeImpl<V : Any>(
    override val id: Key,
    override val defaultValue: V,
    override val requirements: List<Requirement<V>>,
    override val autofillers: List<Autofiller<V>>,
    override val copy: (V) -> V
) : DefaultingContextParamType<V>()

class ContextParamTypeBuilder<V : Any> internal constructor(private val id: Key) {
    
    private val requirements = ArrayList<Requirement<V>>()
    private val autofillers = ArrayList<Autofiller<V>>()
    private var copy: (V) -> V = { it }
    
    private val optionalIntentions = HashSet<ContextIntention>()
    
    fun require(validator: (V) -> Boolean, errorGenerator: (V) -> String): ContextParamTypeBuilder<V> {
        requirements += Requirement(validator, errorGenerator)
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
        autofillers += Autofiller(null, paramTypes, fillValue)
        return this
    }
    
    fun <A : Any> autofilledBy(
        inIntention: ContextIntention,
        lazyParamType: () -> ContextParamType<A>,
        fillValue: (A) -> V?
    ) = autofilledBy(fillValue, lazyParamType)
    
    fun <A : Any, B : Any> autofilledBy(
        inIntention: ContextIntention,
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        fillValue: (A, B) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB)
    
    fun <A : Any, B : Any, C : Any> autofilledBy(
        inIntention: ContextIntention,
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        lazyParamTypeC: () -> ContextParamType<C>,
        fillValue: (A, B, C) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB, lazyParamTypeC)
    
    fun <A : Any, B : Any, C : Any, D : Any> autofilledBy(
        inIntention: ContextIntention,
        lazyParamTypeA: () -> ContextParamType<A>,
        lazyParamTypeB: () -> ContextParamType<B>,
        lazyParamTypeC: () -> ContextParamType<C>,
        lazyParamTypeD: () -> ContextParamType<D>,
        fillValue: (A, B, C, D) -> V?
    ) = autofilledBy(fillValue, lazyParamTypeA, lazyParamTypeB, lazyParamTypeC, lazyParamTypeD)
    
    private fun autofilledBy(
        inIntention: ContextIntention,
        fillValue: Function<V?>,
        vararg lazyParamTypes: () -> ContextParamType<*>
    ): ContextParamTypeBuilder<V> {
        val paramTypes = lazyParamTypes.map(::lazy)
        autofillers += Autofiller(inIntention, paramTypes, fillValue)
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
        for (intention in optionalIntentions) {
            intention.addOptional(type)
        }
    }
    
}