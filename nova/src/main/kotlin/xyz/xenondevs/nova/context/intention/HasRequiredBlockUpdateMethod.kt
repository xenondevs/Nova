@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.HasBlockUpdateMethod.Companion.BLOCK_UPDATE_METHOD
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.block.BlockUpdateMethod

/**
 * A [ContextIntention] that has a [BlockUpdateMethod].
 */
interface HasBlockUpdateMethod<I : HasBlockUpdateMethod<I>> : ContextIntention<I> {
    
    /**
     * The [BlockUpdateMethod] that is used.
     */
    val BLOCK_UPDATE_METHOD: DefaultingContextParamType<BlockUpdateMethod, I>
        get() = blockUpdateMethod()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val BLOCK_UPDATE_METHOD = DefaultingContextParamType<BlockUpdateMethod, Nothing>(
            novaKey("block_update_method"),
            BlockUpdateMethod.WITH_BLOCK_UPDATES
        )
        
        /**
         * Gets the param type for [BLOCK_UPDATE_METHOD].
         */
        fun <I : HasBlockUpdateMethod<I>> blockUpdateMethod() =
            BLOCK_UPDATE_METHOD as DefaultingContextParamType<BlockUpdateMethod, I>
        
    }
    
}