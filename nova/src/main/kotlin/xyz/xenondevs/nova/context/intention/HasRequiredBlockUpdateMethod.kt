@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.HasBlockUpdateFlags.Companion.BLOCK_UPDATE_FLAGS
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.block.BlockUpdateFlags

/**
 * A [ContextIntention] that has [BlockUpdateFlags].
 */
interface HasBlockUpdateFlags<I : HasBlockUpdateFlags<I>> : ContextIntention<I> {
    
    /**
     * The [BlockUpdateFlags] that are used.
     */
    val BLOCK_UPDATE_FLAGS: DefaultingContextParamType<BlockUpdateFlags, I>
        get() = blockUpdateFlags()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val BLOCK_UPDATE_FLAGS = DefaultingContextParamType<BlockUpdateFlags, Nothing>(
            novaKey("block_update_method"),
            BlockUpdateFlags.ALL
        )
        
        /**
         * Gets the param type for [BLOCK_UPDATE_FLAGS].
         */
        fun <I : HasBlockUpdateFlags<I>> blockUpdateFlags() =
            BLOCK_UPDATE_FLAGS as DefaultingContextParamType<BlockUpdateFlags, I>
        
    }
    
}