@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState

/**
 * A [ContextIntention] that has required parameters about a block in a world.
 */
interface HasRequiredBlock<I : HasRequiredBlock<I>> : ContextIntention<I> {
    
    /**
     * The position of a block.
     *
     * Autofilled by: none
     */
    val BLOCK_POS: RequiredContextParamType<BlockPos, I>
    
    /**
     * The world of a block.
     *
     * Autofilled by:
     * - [BLOCK_POS]
     */
    val BLOCK_WORLD: RequiredContextParamType<World, I>
    
    /**
     * The block type as id.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * - [BLOCK_TYPE_VANILLA]
     */
    val BLOCK_TYPE: RequiredContextParamType<Key, I>
    
    /**
     * The vanilla block type.
     *
     * Autofilled by:
     * - [BLOCK_TYPE] if vanilla block
     */
    val BLOCK_TYPE_VANILLA: ContextParamType<BlockType, I>
    
    // TODO: block state vanilla
    // TODO: tile entity vanilla
    
    /**
     * The custom block type.
     *
     * Autofilled by:
     * - [BLOCK_TYPE] if Nova block
     */
    val BLOCK_TYPE_NOVA: ContextParamType<NovaBlock, I>
    
    /**
     * The custom block state.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     *
     * Autofills:
     * - [BLOCK_TYPE_NOVA]
     */
    val BLOCK_STATE_NOVA: ContextParamType<NovaBlockState, I>
    
    companion object {
        
        /**
         * Creates param type for [BLOCK_POS].
         */
        fun <I : HasRequiredBlock<I>> blockPos() =
            RequiredContextParamType<BlockPos, I>(Key(Nova, "block_pos"))
        
        /**
         * Creates a param type for [BLOCK_WORLD].
         */
        fun <I : HasRequiredBlock<I>> blockWorld() =
            RequiredContextParamType<World, I>(Key(Nova, "block_world"))
        
        /**
         * Creates a param type for [BLOCK_TYPE].
         */
        fun <I : HasRequiredBlock<I>> blockType() =
            RequiredContextParamType<Key, I>(Key(Nova, "block_type"))
        
        /**
         * Creates a param type for [BLOCK_TYPE_VANILLA].
         */
        fun <I : HasRequiredBlock<I>> blockTypeVanilla() =
            ContextParamType<BlockType, I>(Key(Nova, "block_type_vanilla"))
        
        /**
         * Creates a param type for [BLOCK_TYPE_NOVA].
         */
        fun <I : HasRequiredBlock<I>> blockTypeNova() =
            ContextParamType<NovaBlock, I>(Key(Nova, "block_type_nova"))
        
        /**
         * Creates a param type for [BLOCK_STATE_NOVA].
         */
        fun <I : HasRequiredBlock<I>> blockStateNova() =
            ContextParamType<NovaBlockState, I>(Key(Nova, "block_state_nova"))
        
        /**
         * Applies the default required properties and autofillers on [intention].
         */
        fun <I : HasRequiredBlock<I>> applyDefaults(intention: HasRequiredBlock<I>) = intention.apply {
            require(BLOCK_POS)
            require(BLOCK_WORLD)
            require(BLOCK_TYPE)
            addAutofiller(BLOCK_WORLD, Autofiller.from(BLOCK_POS, BlockPos::world))
            addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_TYPE_NOVA, NovaBlock::id))
            addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_TYPE_VANILLA, BlockType::key))
            addAutofiller(BLOCK_TYPE_VANILLA, Autofiller.from(BLOCK_TYPE, Registry.BLOCK::get))
            addAutofiller(BLOCK_TYPE_NOVA, Autofiller.from(BLOCK_TYPE, NovaRegistries.BLOCK::getValue))
            addAutofiller(BLOCK_STATE_NOVA, Autofiller.from(BLOCK_TYPE_NOVA, NovaBlock::defaultBlockState))
        }
        
    }
    
}

