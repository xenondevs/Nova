@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_STATE
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_TYPE
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_WORLD
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.block.blockType

/**
 * A [ContextIntention] that has required parameters about a block in a world.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK_WORLD] | 1. | [BLOCK] | |
 * | [BLOCK_TYPE] | 1. | [BLOCK_TYPE_NOVA] | |
 * | | 2. | [BLOCK_TYPE_VANILLA] | |
 * | [BLOCK_TYPE_VANILLA] | 1. | [BLOCK_STATE] | |
 * | | 2. | [BLOCK_TYPE] | Only if vanilla block |
 * | [BLOCK_TYPE_NOVA] | 1. | [BLOCK_STATE_NOVA] | |
 * | | 2. | [BLOCK_TYPE] | Only if Nova block |
 */
interface HasRequiredBlock<I : HasRequiredBlock<I>> : ContextIntention<I> {
    
    /**
     * The block at a position in a world.
     */
    val BLOCK: RequiredContextParamType<Block, I>
        get() = block()
    
    /**
     * The world of a block.
     */
    val BLOCK_WORLD: RequiredContextParamType<World, I>
        get() = blockWorld()
    
    /**
     * The block type as id.
     */
    val BLOCK_TYPE: RequiredContextParamType<BlockType, I>
        get() = blockType()
    
    /**
     * The block data (block state).
     */
    val BLOCK_STATE: RequiredContextParamType<BlockData, I>
        get() = blockState()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val BLOCK = RequiredContextParamType<Block, Nothing>(novaKey("block"))
        private val BLOCK_WORLD = RequiredContextParamType<World, Nothing>(novaKey("block_world"))
        private val BLOCK_TYPE = RequiredContextParamType<BlockType, Nothing>(novaKey("block_type"))
        private val BLOCK_STATE = RequiredContextParamType<BlockData, Nothing>(novaKey("block_state"))
        
        /**
         * Gets the param type for [BLOCK].
         */
        fun <I : HasRequiredBlock<I>> block() =
            BLOCK as RequiredContextParamType<Block, I>
        
        /**
         * Gets the param type for [BLOCK_WORLD].
         */
        fun <I : HasRequiredBlock<I>> blockWorld() =
            BLOCK_WORLD as RequiredContextParamType<World, I>
        
        /**
         * Gets the param type for [BLOCK_TYPE].
         */
        fun <I : HasRequiredBlock<I>> blockType() =
            BLOCK_TYPE as RequiredContextParamType<BlockType, I>
        
        /**
         * Gets the param type for [BLOCK_STATE].
         */
        fun <I : HasRequiredBlock<I>> blockState() =
            BLOCK_STATE as RequiredContextParamType<BlockData, I>

        /**
         * Applies the default required properties and autofillers on [intention].
         */
        fun <I : HasRequiredBlock<I>> applyDefaults(intention: HasRequiredBlock<I>) = intention.apply {
            require(BLOCK)
            require(BLOCK_WORLD)
            require(BLOCK_TYPE)
            addAutofiller(BLOCK_WORLD, Autofiller.from(BLOCK, Block::getWorld))
            addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_STATE) { it.blockType })
        }
        
    }
    
}

