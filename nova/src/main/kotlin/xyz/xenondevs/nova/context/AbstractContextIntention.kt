package xyz.xenondevs.nova.context

/**
 * Abstract implementation of [ContextIntention].
 */
abstract class AbstractContextIntention<I : AbstractContextIntention<I>> : ContextIntention<I> {
    
    private val autofillers = HashMap<ContextParamType<*, I>, ArrayList<Autofiller<*, I>>>()
    final override val required: Set<RequiredContextParamType<*, I>>
        field = HashSet<RequiredContextParamType<*, I>>()
    
    override fun require(paramType: RequiredContextParamType<*, I>) {
        required += paramType
    }
    
    override fun <V : Any> addAutofiller(
        paramType: ContextParamType<V, I>,
        autofiller: Autofiller<V, I>,
        at: Int
    ) {
        autofillers.getOrPut(paramType, ::ArrayList)
            .apply { add(at.coerceIn(0..size), autofiller) }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <V : Any> getAutofillers(paramType: ContextParamType<V, I>): List<Autofiller<V, I>> {
        return autofillers[paramType] as? List<Autofiller<V, I>> ?: emptyList()
    }
    
}