package xyz.xenondevs.nova.context.intention

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.GameMode
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.util.novaKey

/**
 * A [ContextIntention] for when a block is broken.
 *
 * ## Autofillers
 *
 * Inherits autofillers from [HasRequiredBlock], [HasOptionalTileEntity], [HasOptionalSource],
 * [HasOptionalBlockInteraction], and [HasHeldItem].
 *
 * | Target | # | Source(s) | Notes |
 * |--------|---|-----------|-------|
 * | [TOOL_ITEM_STACK] | 1. | [HELD_ITEM_STACK] | Only if tool |
 * | [BLOCK_DROPS] | 1. | [BLOCK], [TOOL_ITEM_STACK], [SOURCE_PLAYER] | |
 * | | 2. | [BLOCK], [SOURCE_PLAYER] | |
 * | | 3. | [BLOCK] | |
 * | [BLOCK_EXP_DROPS] | 1. | [BLOCK_DROPS] | |
 * | [BLOCK_STATE_NOVA] | +1. | [BLOCK] | Only if Nova block |
 * | [BLOCK_STATE] | +1. | [BLOCK] | Only if vanilla block |
 */
object BlockBreak :
    AbstractContextIntention<BlockBreak>(),
    HasRequiredBlock<BlockBreak>,
    HasBlockUpdateFlags<BlockBreak>,
    HasOptionalTileEntity<BlockBreak>,
    HasOptionalSource<BlockBreak>,
    HasOptionalBlockInteraction<BlockBreak>,
    HasHeldItem<BlockBreak> {
        
   // TODO: decide on whether or not to keep this. There is a problem with this in that this information cannot be autofilled depending on when the ctx is created
   
    /**
     * The block data (block state) that is replacing this block.
     */
    val NEW_BLOCK_STATE = DefaultingContextParamType<BlockData, BlockPlace>(
        novaKey("new_block_state"),
        default = BlockType.AIR.createBlockData()
    )
        
    /**
     * The item stack used as a tool.
     * Defaults to an empty item stack.
     */
    val TOOL_ITEM_STACK = DefaultingContextParamType<ItemStack, BlockBreak>(
        novaKey("tool_item_stack"),
        default = ItemStack.empty(),
        copy = ItemStack::clone
    )
    
    /**
     * Whether block drops should be dropped.
     * Defaults to `false`.
     *
     * @see BLOCK_STORAGE_DROPS
     * @see BLOCK_EXP_DROPS
     */
    val BLOCK_DROPS = DefaultingContextParamType<Boolean, BlockBreak>(
        novaKey("block_drops"),
        default = false
    )
    
    /**
     * Whether block storage drops should be dropped.
     * Defaults to `true`
     *
     * @see BLOCK_DROPS
     * @see BLOCK_EXP_DROPS
     */
    val BLOCK_STORAGE_DROPS = DefaultingContextParamType<Boolean, BlockBreak>(
        novaKey("block_storage_drops"),
        default = true
    )
    
    /**
     * Whether block exp orbs should be spawned.
     * Defaults to `false`
     *
     * @see BLOCK_DROPS
     * @see BLOCK_STORAGE_DROPS
     */
    val BLOCK_EXP_DROPS = DefaultingContextParamType<Boolean, BlockBreak>(
        novaKey("block_exp_drops"),
        default = false
    )
    
    /**
     * Whether block break effects should be played.
     * Defaults to `true`
     */
    val BLOCK_BREAK_EFFECTS = DefaultingContextParamType<Boolean, BlockBreak>(
        novaKey("block_break_effects"),
        default = true
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalTileEntity.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(TOOL_ITEM_STACK, Autofiller.from(HELD_ITEM_STACK) { if (it.hasData(DataComponentTypes.TOOL)) it else null })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK, TOOL_ITEM_STACK, SOURCE_PLAYER) { block, tool, player -> player.gameMode != GameMode.CREATIVE && block.isPreferredTool(tool) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK, SOURCE_PLAYER) { block, player -> player.gameMode != GameMode.CREATIVE && block.isPreferredTool(ItemStack.empty()) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK) { block -> block.isPreferredTool(ItemStack.empty()) })
        addAutofiller(BLOCK_EXP_DROPS, Autofiller.from(BLOCK_DROPS) { it })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_STATE, Autofiller.from(BLOCK) { it.blockData })
    }
    
}
