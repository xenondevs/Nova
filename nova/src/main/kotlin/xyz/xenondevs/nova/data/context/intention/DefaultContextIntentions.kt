package xyz.xenondevs.nova.data.context.intention

/**
 * Contains all built-in [context intentions][ContextIntention].
 */
object DefaultContextIntentions {
    
    /**
     * The intention to place a block.
     */
    data object BlockPlace : ContextIntention()
    
    /**
     * The intention to break a block.
     */
    data object BlockBreak : ContextIntention()
    
    /**
     * The intention to interact with a block.
     */
    data object BlockInteract : ContextIntention()
    
}