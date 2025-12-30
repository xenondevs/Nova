package xyz.xenondevs.nova.context

import net.kyori.adventure.key.Key

/**
 * A context parameter type.
 */
open class ContextParamType<V : Any, I : ContextIntention<I>>(
    /**
     * The id of this parameter type, should be unique for [I].
     */
    val id: Key,
    
    /**
     * Validates whether a value meets the requirements of this parameter type.
     */
    val validate: (V) -> Boolean = { true },
    
    /**
     * Copies a value.
     */
    val copy: (V) -> V = { it }
) {
    
    override fun equals(other: Any?): Boolean =
        this === other || (other is ContextParamType<*, *> && id == other.id)
    
    override fun hashCode(): Int =
        id.hashCode()
    
    override fun toString(): String =
        id.toString()
    
}

/**
 * A context parameter type that has a default value instead of null.
 */
class DefaultingContextParamType<V : Any, I : ContextIntention<I>>(
    id: Key,
    /**
     * The default value of this parameter type.
     * Used when no explicit value is defined and no autofiller could fill it.
     */
    val default: V,
    validate: (V) -> Boolean = { true },
    copy: (V) -> V = { it }
) : ContextParamType<V, I>(id, validate, copy)

/**
 * A context parameter type that is required in the intention it belongs to.
 */
class RequiredContextParamType<V : Any, I : ContextIntention<I>>(
    id: Key,
    validate: (V) -> Boolean = { true },
    copy: (V) -> V = { it }
) : ContextParamType<V, I>(id, validate, copy)