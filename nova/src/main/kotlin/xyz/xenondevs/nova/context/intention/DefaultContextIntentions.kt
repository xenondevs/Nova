package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.param.DefaultContextParamTypes.BLOCK_POS
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes.BLOCK_WORLD

/**
 * Contains all built-in [context intentions][ContextIntention].
 */
object DefaultContextIntentions {
    
    /**
     * The intention to place a block.
     * 
     * Required param types:
     * - [BLOCK_POS]
     * - [BLOCK_WORLD]
     */
    data object BlockPlace : ContextIntention() {
        override val required by lazy { setOf(BLOCK_POS, BLOCK_WORLD) }
    }
    
    /**
     * The intention to break a block.
     * 
     * Required param types:
     * - [BLOCK_POS]
     * - [BLOCK_WORLD]
     */
    data object BlockBreak : ContextIntention() {
        override val required by lazy { setOf(BLOCK_POS, BLOCK_WORLD) }
    }
    
    /**
     * The intention to interact with a block.
     *
     * Required param types:
     * - [BLOCK_POS]
     * - [BLOCK_WORLD]
     */
    data object BlockInteract : ContextIntention() {
        override val required by lazy { setOf(BLOCK_POS, BLOCK_WORLD) }
    }
    
}