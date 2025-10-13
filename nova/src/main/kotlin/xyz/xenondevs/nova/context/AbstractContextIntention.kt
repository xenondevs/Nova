package xyz.xenondevs.nova.context

/**
 * Abstract implementation of [ContextIntention].
 */
abstract class AbstractContextIntention<I : AbstractContextIntention<I>> : ContextIntention<I> {
    
    private val _required = HashSet<RequiredContextParamType<*, I>>()
    private val _autofillers = HashMap<ContextParamType<*, I>, ArrayList<Autofiller<*, I>>>()
    override val required: Set<RequiredContextParamType<*, I>>
        get() = _required
    
    override fun require(paramType: RequiredContextParamType<*, I>) {
        _required += paramType
    }
    
    override fun <V : Any> addAutofiller(
        paramType: ContextParamType<V, I>,
        autofiller: Autofiller<V, I>,
        at: Int
    ) {
        _autofillers.getOrPut(paramType, ::ArrayList)
            .apply { add(at.coerceIn(0..size), autofiller) }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getAutofillers(paramType: ContextParamType<V, I>): List<Autofiller<V, I>> {
        return _autofillers[paramType] as? List<Autofiller<V, I>> ?: emptyList()
    }
    
}