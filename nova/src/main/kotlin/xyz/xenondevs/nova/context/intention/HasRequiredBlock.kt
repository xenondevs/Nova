@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_POS
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_STATE_NOVA
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_STATE_VANILLA
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_TYPE
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.context.intention.HasRequiredBlock.Companion.BLOCK_WORLD
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.novaBlock
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.pos

/**
 * A [ContextIntention] that has required parameters about a block in a world.
 *
 * ## Autofillers
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK] | 1. | [BLOCK_POS] | |
 * | [BLOCK_POS] | 1. | [BLOCK] | |
 * | [BLOCK_WORLD] | 1. | [BLOCK_POS] | |
 * | [BLOCK_TYPE] | 1. | [BLOCK_TYPE_NOVA] | |
 * | | 2. | [BLOCK_TYPE_VANILLA] | |
 * | [BLOCK_TYPE_VANILLA] | 1. | [BLOCK_STATE_VANILLA] | |
 * | | 2. | [BLOCK_TYPE] | Only if vanilla block |
 * | [BLOCK_STATE_VANILLA] | 1. | [BLOCK_POS] | Only if not Nova block |
 * | [BLOCK_TYPE_NOVA] | 1. | [BLOCK_STATE_NOVA] | |
 * | | 2. | [BLOCK_TYPE] | Only if Nova block |
 * | [BLOCK_STATE_NOVA] | 1. | [BLOCK_POS] | Only if Nova block |
 */
interface HasRequiredBlock<I : HasRequiredBlock<I>> : ContextIntention<I> {
    
    /**
     * The block at a position in a world.
     */
    val BLOCK: RequiredContextParamType<Block, I>
        get() = block()
    
    /**
     * The position of a block.
     */
    val BLOCK_POS: RequiredContextParamType<BlockPos, I>
        get() = blockPos()
    
    /**
     * The world of a block.
     */
    val BLOCK_WORLD: RequiredContextParamType<World, I>
        get() = blockWorld()
    
    /**
     * The block type as id.
     */
    val BLOCK_TYPE: RequiredContextParamType<Key, I>
        get() = blockType()
    
    /**
     * The vanilla block type.
     */
    val BLOCK_TYPE_VANILLA: ContextParamType<BlockType, I>
        get() = blockTypeVanilla()
    
    /**
     * The vanilla block data (block state).
     */
    val BLOCK_STATE_VANILLA: ContextParamType<BlockData, I>
        get() = blockStateVanilla()
    
    /**
     * The custom block type.
     */
    val BLOCK_TYPE_NOVA: ContextParamType<NovaBlock, I>
        get() = blockTypeNova()
    
    /**
     * The custom block state.
     */
    val BLOCK_STATE_NOVA: ContextParamType<NovaBlockState, I>
        get() = blockStateNova()
    
    @Suppress("UNCHECKED_CAST")
    companion object {
        
        private val BLOCK = RequiredContextParamType<Block, Nothing>(Key(Nova, "block"))
        private val BLOCK_POS = RequiredContextParamType<BlockPos, Nothing>(Key(Nova, "block_pos"))
        private val BLOCK_WORLD = RequiredContextParamType<World, Nothing>(Key(Nova, "block_world"))
        private val BLOCK_TYPE = RequiredContextParamType<Key, Nothing>(Key(Nova, "block_type"))
        private val BLOCK_TYPE_VANILLA = ContextParamType<BlockType, Nothing>(Key(Nova, "block_type_vanilla"))
        private val BLOCK_STATE_VANILLA = ContextParamType<BlockData, Nothing>(Key(Nova, "block_state_vanilla"))
        private val BLOCK_TYPE_NOVA = ContextParamType<NovaBlock, Nothing>(Key(Nova, "block_type_nova"))
        private val BLOCK_STATE_NOVA = ContextParamType<NovaBlockState, Nothing>(Key(Nova, "block_state_nova"))
        
        /**
         * Gets the param type for [BLOCK].
         */
        fun <I : HasRequiredBlock<I>> block() =
            BLOCK as RequiredContextParamType<Block, I>
        
        /**
         * Gets the param type for [BLOCK_POS].
         */
        fun <I : HasRequiredBlock<I>> blockPos() =
            BLOCK_POS as RequiredContextParamType<BlockPos, I>
        
        /**
         * Gets the param type for [BLOCK_WORLD].
         */
        fun <I : HasRequiredBlock<I>> blockWorld() =
            BLOCK_WORLD as RequiredContextParamType<World, I>
        
        /**
         * Gets the param type for [BLOCK_TYPE].
         */
        fun <I : HasRequiredBlock<I>> blockType() =
            BLOCK_TYPE as RequiredContextParamType<Key, I>
        
        /**
         * Gets the param type for [BLOCK_TYPE_VANILLA].
         */
        fun <I : HasRequiredBlock<I>> blockTypeVanilla() =
            BLOCK_TYPE_VANILLA as ContextParamType<BlockType, I>
        
        /**
         * Gets the param type for [BLOCK_STATE_VANILLA].
         */
        fun <I : HasRequiredBlock<I>> blockStateVanilla() =
            BLOCK_STATE_VANILLA as ContextParamType<BlockData, I>
        
        /**
         * Gets the param type for [BLOCK_TYPE_NOVA].
         */
        fun <I : HasRequiredBlock<I>> blockTypeNova() =
            BLOCK_TYPE_NOVA as ContextParamType<NovaBlock, I>
        
        /**
         * Gets the param type for [BLOCK_STATE_NOVA].
         */
        fun <I : HasRequiredBlock<I>> blockStateNova() =
            BLOCK_STATE_NOVA as ContextParamType<NovaBlockState, I>
        
        /**
         * Applies the default required properties and autofillers on [intention].
         */
        fun <I : HasRequiredBlock<I>> applyDefaults(intention: HasRequiredBlock<I>) = intention.apply {
            require(BLOCK)
            require(BLOCK_POS)
            require(BLOCK_WORLD)
            require(BLOCK_TYPE)
            addAutofiller(BLOCK, Autofiller.from(BLOCK_POS, BlockPos::block))
            addAutofiller(BLOCK_POS, Autofiller.from(BLOCK, Block::pos))
            addAutofiller(BLOCK_WORLD, Autofiller.from(BLOCK_POS, BlockPos::world))
            addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_TYPE_NOVA, NovaBlock::id))
            addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_TYPE_VANILLA, BlockType::key))
            addAutofiller(BLOCK_TYPE_VANILLA, Autofiller.from(BLOCK_STATE_VANILLA) { it.material.asBlockType() })
            addAutofiller(BLOCK_TYPE_VANILLA, Autofiller.from(BLOCK_TYPE, Registry.BLOCK::get))
            addAutofiller(BLOCK_STATE_VANILLA, Autofiller.from(BLOCK_POS) { if (it.block.novaBlock == null) it.block.blockData else null })
            addAutofiller(BLOCK_TYPE_NOVA, Autofiller.from(BLOCK_STATE_NOVA, NovaBlockState::block))
            addAutofiller(BLOCK_TYPE_NOVA, Autofiller.from(BLOCK_TYPE, NovaRegistries.BLOCK::getValue))
            addAutofiller(BLOCK_STATE_NOVA, Autofiller.from(BLOCK_POS) { it.novaBlockState })
        }
        
    }
    
}

