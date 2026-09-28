package xyz.xenondevs.nova.context

/**
 * A context parameter type.
 */
open class ContextParamType<V : Any, I : ContextIntention<I>>(
    /**
     * The id of this parameter type, should be unique for [I].
     */
    val id: Int,
    
    /**
     * Validates whether a value meets the requirements of this parameter type.
     */
    val validate: (V) -> Boolean = { true },
    
    /**
     * Copies a value.
     */
    val copy: (V) -> V = { it }
)

/**
 * A context parameter type that has a default value instead of null.
 */
class DefaultingContextParamType<V : Any, I : ContextIntention<I>>(
    id: Int,
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
    id: Int,
    validate: (V) -> Boolean = { true },
    copy: (V) -> V = { it }
) : ContextParamType<V, I>(id, validate, copy)