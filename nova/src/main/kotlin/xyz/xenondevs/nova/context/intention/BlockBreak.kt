package xyz.xenondevs.nova.context.intention

import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.context.RequiredContextParamType
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_EXP_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_POS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_STORAGE_DROPS
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_TYPE_NOVA
import xyz.xenondevs.nova.context.intention.BlockBreak.BLOCK_TYPE_VANILLA
import xyz.xenondevs.nova.context.intention.BlockBreak.INTERACTION_ITEM_STACK
import xyz.xenondevs.nova.context.intention.BlockBreak.SOURCE_PLAYER
import xyz.xenondevs.nova.context.intention.BlockBreak.TOOL_ITEM_STACK
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.item.ToolUtils

/**
 * Context intention for when a block is broken.
 */
object BlockBreak : Block<BlockBreak>() {
    
    /**
     * The block type as id.
     *
     * Autofilled by:
     * - [BLOCK_TYPE_NOVA]
     * - [BLOCK_TYPE_VANILLA]
     * - [BLOCK_POS]
     */
    override val BLOCK_TYPE: RequiredContextParamType<Key, BlockBreak> =
        HasRequiredBlock.blockType()
    
    /**
     * The item stack used as a tool.
     * Defaults to an empty item stack.
     *
     * Autofilled by:
     * - [INTERACTION_ITEM_STACK]
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
     * Autofilled by:
     * - [BLOCK_POS] with and without [TOOL_ITEM_STACK] with and without [SOURCE_PLAYER]
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
     * Autofilled by: none
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
     * Autofilled by:
     * - [BLOCK_DROPS]
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
     *
     * Autofilled by: none
     */
    val BLOCK_BREAK_EFFECTS = DefaultingContextParamType<Boolean, BlockBreak>(
        Key(Nova, "block_break_effects"),
        default = true
    )
    
    init {
        applyDefaults(this)
        
        addAutofiller(TOOL_ITEM_STACK, Autofiller.from(INTERACTION_ITEM_STACK) { it })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS, TOOL_ITEM_STACK, SOURCE_PLAYER) { pos, tool, player -> player.gameMode != GameMode.CREATIVE && ToolUtils.isCorrectToolForDrops(pos.block, tool) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS, SOURCE_PLAYER) { pos, player -> player.gameMode != GameMode.CREATIVE && ToolUtils.isCorrectToolForDrops(pos.block, null) })
        addAutofiller(BLOCK_DROPS, Autofiller.from(BLOCK_POS) { pos -> ToolUtils.isCorrectToolForDrops(pos.block, null) })
        addAutofiller(BLOCK_EXP_DROPS, Autofiller.from(BLOCK_DROPS) { it })
        
        // extra autofillers for inherited properties
        addAutofiller(BLOCK_TYPE, Autofiller.from(BLOCK_POS) { it.block.id })
    }
    
}