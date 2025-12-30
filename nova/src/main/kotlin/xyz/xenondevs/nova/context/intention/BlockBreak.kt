package xyz.xenondevs.nova.context.intention

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.AbstractContextIntention
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextIntention
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_EXP_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_POS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_STORAGE_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_TYPE
import xyz.xenondevs.nova.context.intention.BlockBreak.HELD_ITEM_STACK
import xyz.xenondevs.nova.context.intention.BlockBreak.SOURCE_PLAYER
import xyz.xenondevs.nova.context.intention.BlockBreak.TOOL_ITEM_STACK
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.world.item.tool.ToolCategory


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
 * | [BLOCK_TYPE] | +1. | [BLOCK_POS] | |
 * | [TOOL_ITEM_STACK] | 1. | [HELD_ITEM_STACK] | Only if tool |
 * | [BLOCK_DROPS] | 1. | [BLOCK_POS], [TOOL_ITEM_STACK], [SOURCE_PLAYER] | |
 * | | 2. | [BLOCK_POS], [SOURCE_PLAYER] | |
 * | | 3. | [BLOCK_POS] | |
 * | [BLOCK_EXP_DROPS] | 1. | [BLOCK_DROPS] | |
 */
object BlockBreak :
    AbstractContextIntention<BlockBreak>(),
    HasRequiredBlock<BlockBreak>,
    HasOptionalTileEntity<BlockBreak>,
    HasOptionalSource<BlockBreak>,
    HasOptionalBlockInteraction<BlockBreak>,
    HasHeldItem<BlockBreak> {
    
    /**
     * The item stack used as a tool.
     * Defaults to an empty item stack.
     */
    val TOOL_ITEM_STACK = DefaultingContextParamType<ItemStack, BlockBreak>(
        Key(Nova, "tool_item_stack"),
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
        Key(Nova, "block_drops"),
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
        Key(Nova, "block_storage_drops"),
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
        Key(Nova, "block_exp_drops"),
        default = false
    )
    
    /**
     * Whether block break effects should be played.
     * Defaults to `true`
     */
    val BLOCK_BREAK_EFFECTS = DefaultingContextParamType<Boolean, BlockBreak>(
        Key(Nova, "block_break_effects"),
        default = true
    )
    
    init {
        HasRequiredBlock.applyDefaults(this)
        HasOptionalTileEntity.applyDefaults(this)
        HasOptionalSource.applyDefaults(this)
        HasOptionalBlockInteraction.applyDefaults(this)
        HasHeldItem.applyDefaults(this)
        
        addAutofiller(TOOL_ITEM_STACK, Autofiller.from(HELD_ITEM_STACK) { if (it.hasData(DataComponentTypes.TOOL) || ToolCategory.ofItem(it).isNotEmpty()) it else null })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS, TOOL_ITEM_STACK, SOURCE_PLAYER) { pos, tool, player -> player.gameMode != GameMode.CREATIVE && ToolUtils.isCorrectToolForDrops(pos.block, tool) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS, SOURCE_PLAYER) { pos, player -> player.gameMode != GameMode.CREATIVE && ToolUtils.isCorrectToolForDrops(pos.block, null) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS) { pos -> ToolUtils.isCorrectToolForDrops(pos.block, null) })
        addAutofiller(BLOCK_EXP_DROPS, Autofiller.from(BLOCK_DROPS) { it })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_POS) { it.block.id })
    }
    
}