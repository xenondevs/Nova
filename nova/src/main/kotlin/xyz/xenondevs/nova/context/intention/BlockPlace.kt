package xyz.xenondevs.nova.context.intention

import net.minecraft.core.component.DataComponents
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.blockTypeOrNull
import xyz.xenondevs.nova.world.block.novaBlock
import xyz.xenondevs.nova.world.item.itemType
import xyz.xenondevs.nova.world.item.novaItem
import kotlin.jvm.optionals.getOrNull

/**
 * A [ContextIntention] for when a block is placed.
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasRequiredBlock], [HasOptionalTileEntityData], [HasOptionalSource],
 * [HasOptionalBlockInteraction], and [HasHeldItem].
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [BLOCK_ITEM_STACK] | 1. | [HELD_ITEM_STACK] | Only if block item |
 * | | 2. | [BLOCK_TYPE_NOVA] | Only if has item type |
 * | | 3. | [BLOCK_TYPE_VANILLA] | Only if has item type |
 * | [BLOCK_TYPE] | +1. | [BLOCK_ITEM_STACK] | |
 * | [BLOCK_STATE_NOVA] | +1. | Entire context | Determined via [NovaBlock.chooseBlockState] |
 * | [BLOCK_STATE] | +1. | [BLOCK_TYPE_VANILLA] | Uses default block state. This may be changed to the proper placement block state in the future.
 */
object BlockPlace :
    AbstractContextIntention<BlockPlace>(),
    HasRequiredBlock<BlockPlace>,
    HasBlockUpdateFlags<BlockPlace>,
    HasOptionalTileEntityData<BlockPlace>,
    HasOptionalSource<BlockPlace>,
    HasOptionalBlockInteraction<BlockPlace>,
    HasHeldItem<BlockPlace> {
    
    /**
     * The block data (block state) that was replaced by placing this block.
     */
    val PREVIOUS_BLOCK_STATE = DefaultingContextParamType<BlockData, BlockPlace>(
        novaKey("previous_block_state"), 
        default = BlockType.AIR.createBlockData()
    )
        
    /**
     * The item stack to be placed as a block.
     */
    val BLOCK_ITEM_STACK = ContextParamType<ItemStack, BlockPlace>(
        novaKey("block_item_stack"),
        validate = { it.type.isBlock || it.novaItem?.block != null },
        copy = ItemStack::clone
    )
    
    /**
     * Whether block place effects should be played.
     * Defaults to `true`.
     */
    val BLOCK_PLACE_EFFECTS = DefaultingContextParamType<Boolean, BlockPlace>(
        novaKey("block_place_effects"),
        default = true
    )
    
    /**
     * Whether tile-entity limits should be bypassed when placing tile-entity blocks.
     * Placed blocks will still be counted.
     * Defaults to `false`.
     */
    val BYPASS_TILE_ENTITY_LIMITS = DefaultingContextParamType<Boolean, BlockPlace>(
        novaKey("bypass_tile_entity_limits"),
        default = false
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(HELD_ITEM_STACK) { it.takeIf { it.itemType.hasBlockType() } })
        addAutofiller(BLOCK_ITEM_STACK, Autofiller.from(BLOCK_TYPE) { if (it.hasItemType()) it.itemType.createItemStack() else null })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_ITEM_STACK) { it.itemType.blockTypeOrNull })
        addAutofiller(BLOCK_STATE, Autofiller.fromContext { ctx ->
            val blockType = ctx[BLOCK_TYPE]
            blockType.novaBlock?.chooseBlockState(ctx) ?: blockType.createBlockData() // TODO: choose correct vanilla block state as well
        })
        
        @Suppress("DEPRECATION")
        addAutofiller(TILE_ENTITY_DATA_NOVA, Autofiller.from(BLOCK_ITEM_STACK) {
            it.unwrap()
                .get(DataComponents.BLOCK_ENTITY_DATA)
                ?.unsafe
                ?.getByteArray("nova")
                ?.getOrNull()
                ?.let(Cbf::read)
        })
    }
    
}
