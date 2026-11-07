package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsInteractionHand
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.toNova
import net.minecraft.world.InteractionResult as NmsInteractionResult

/**
 * Runs vanilla item use logic ([interaction]) for the [itemStack] that [player] holds in [hand].
 *
 * Afterward, the held item is reset to [itemStack] so that component-based post-use effects in
 * [InteractionResult.Success.performActions] work correctly. This asserts that transformations are
 * stored in the interaction result via `transformedTo`.
 */
internal inline fun useVanilla(
    player: Player,
    hand: EquipmentSlot,
    itemStack: ItemStack,
    interaction: (serverPlayer: ServerPlayer, hand: InteractionHand) -> NmsInteractionResult
): InteractionResult {
    val result = interaction(player.serverPlayer, hand.nmsInteractionHand).toNova()
    player.equipment.setItem(hand, itemStack)
    return result
}

/**
 * Runs vanilla item use-on-block logic ([interaction]) for the [itemStack] that [player] holds in [hand],
 * used on the [face] of [block].
 *
 * Afterward, the held item is reset to [itemStack] and its transformation (e.g. damage) is moved into the
 * interaction result as an [ItemAction.ConvertStack], so that post-use effects in
 * [InteractionResult.Success.performActions] work correctly.
 */
internal inline fun useVanillaOnBlock(
    player: Player,
    hand: EquipmentSlot,
    itemStack: ItemStack,
    block: Block,
    face: BlockFace,
    interaction: (context: UseOnContext) -> NmsInteractionResult
): InteractionResult {
    val context = UseOnContext(
        player.serverPlayer,
        hand.nmsInteractionHand,
        BlockHitResult(Vec3.atCenterOf(block.nmsPos), face.nmsDirection, block.nmsPos, false)
    )
    val result = interaction(context).toNova()
    if (result !is InteractionResult.Success)
        return result
    
    val action = ItemAction.ConvertStack(player.equipment.getItem(hand))
    player.equipment.setItem(hand, itemStack)
    return result.copy(action = action)
}

/**
 * Transforms the clicked block using the [block transformer component][DataComponents.BLOCK_TRANSFORMER]
 * of the [itemStack], for behaviors that add such a component.
 */
internal fun useBlockTransformer(itemStack: ItemStack, block: Block, ctx: Context<BlockInteract>): InteractionResult {
    val player = ctx[BlockInteract.SOURCE_PLAYER]
        ?: return InteractionResult.Pass
    val hand = ctx[BlockInteract.HELD_HAND]
        ?: return InteractionResult.Pass
    val face = ctx[BlockInteract.CLICKED_BLOCK_FACE]
        ?: return InteractionResult.Pass
    
    return useVanillaOnBlock(player, hand, itemStack, block, face) { context ->
        context.itemInHand.get(DataComponents.BLOCK_TRANSFORMER)?.value()?.transformBlock(context)
            ?: NmsInteractionResult.PASS
    }
}
