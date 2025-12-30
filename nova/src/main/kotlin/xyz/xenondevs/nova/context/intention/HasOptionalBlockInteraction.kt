@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.intention.HasOptionalBlockInteraction.Companion.CLICKED_BLOCK_FACE
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.Key

/**
 * A [ContextIntention] that has optional parameters about an interaction on or with a block.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [CLICKED_BLOCK_FACE] | 1. | [SOURCE_PLAYER] | |
 */
interface HasOptionalBlockInteraction<I : HasOptionalBlockInteraction<I>> : HasOptionalSource<I> {
    
    /**
     * The face of a block that was clicked.
     */
    val CLICKED_BLOCK_FACE: ContextParamType<BlockFace, I>
        get() = clickedBlockFace()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val CLICKED_BLOCK_FACE = ContextParamType<BlockFace, Nothing>(Key(Nova, "clicked_block_face"))
        
        /**
         * Gets the param type for [CLICKED_BLOCK_FACE]
         */
        fun <I : HasOptionalBlockInteraction<I>> clickedBlockFace() =
            CLICKED_BLOCK_FACE as ContextParamType<BlockFace, I>
        
        fun <I : HasOptionalBlockInteraction<I>> applyDefaults(intention: HasOptionalBlockInteraction<I>) = intention.apply {
            addAutofiller(CLICKED_BLOCK_FACE, Autofiller.from(SOURCE_PLAYER) { BlockFaceUtils.determineBlockFaceLookingAt(it.eyeLocation) })
        }
        
    }
    
}