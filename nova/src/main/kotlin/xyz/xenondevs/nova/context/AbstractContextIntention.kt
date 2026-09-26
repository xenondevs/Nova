package xyz.xenondevs.nova.context

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abstract implementation of [ContextIntention].
 */
abstract class AbstractContextIntention<I : AbstractContextIntention<I>> : ContextIntention<I> {
    
    private val autofillers = HashMap<ContextParamType<*, I>, CopyOnWriteArrayList<Autofiller<*, I>>>()
    final override val requiredParamTypes: List<RequiredContextParamType<*, I>>
        field = CopyOnWriteArrayList<RequiredContextParamType<*, I>>()
    final override val paramTypes: List<ContextParamType<*, I>>
        field = CopyOnWriteArrayList<ContextParamType<*, I>>()
    
    private val nextId = AtomicInteger(0)
    
    override fun <V : Any> addRequiredParamType(
        validate: (V) -> Boolean,
        copy: (V) -> V
    ): RequiredContextParamType<V, I> {
        val type = RequiredContextParamType<V, I>(nextId.getAndIncrement(), validate, copy)
        paramTypes += type
        requiredParamTypes += type
        return type
    }
    
    override fun <V : Any> addDefaultingParamType(
        default: V,
        validate: (V) -> Boolean,
        copy: (V) -> V
    ): DefaultingContextParamType<V, I> {
        require(validate(default)) { "Invalid default value $default for context parameter" }
        val type = DefaultingContextParamType<V, I>(nextId.getAndIncrement(), default, validate, copy)
        paramTypes += type
        return type
    }
    
    override fun <V : Any> addOptionalParamType(
        validate: (V) -> Boolean,
        copy: (V) -> V
    ): ContextParamType<V, I> {
        val type = ContextParamType<V, I>(nextId.getAndIncrement(), validate, copy)
        paramTypes += type
        return type
    }
    
    override fun <V : Any> addAutofiller(
        paramType: ContextParamType<V, I>,
        autofiller: Autofiller<V, I>,
        at: Int
    ) {
        autofillers.getOrPut(paramType, ::CopyOnWriteArrayList)
            .apply { add(at.coerceIn(0..size), autofiller) }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getAutofillers(paramType: ContextParamType<V, I>): List<Autofiller<V, I>> {
        return autofillers[paramType] as? List<Autofiller<V, I>> ?: emptyList()
    }
    
}