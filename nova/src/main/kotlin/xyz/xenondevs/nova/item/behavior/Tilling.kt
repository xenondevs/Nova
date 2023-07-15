package xyz.xenondevs.nova.item.behavior

import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.above
import xyz.xenondevs.nova.util.interactionHand
import xyz.xenondevs.nova.util.nmsDirection
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.pos
import net.minecraft.world.item.ItemStack as MojangStack

private val TILLABLES: Map<Block, Triple<(PlayerInteractEvent) -> Boolean, BlockState, List<Item>>> = mapOf(
    Blocks.GRASS_BLOCK to Triple(::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState(), emptyList()),
    Blocks.DIRT_PATH to Triple(::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState(), emptyList()),
    Blocks.DIRT to Triple(::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState(), emptyList()),
    Blocks.COARSE_DIRT to Triple(::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState(), emptyList()),
    Blocks.ROOTED_DIRT to Triple({ true }, Blocks.DIRT.defaultBlockState(), listOf(Items.HANGING_ROOTS))
)

private fun onlyIfAirAbove(event: PlayerInteractEvent): Boolean {
    return event.blockFace != BlockFace.DOWN && event.clickedBlock!!.above.type.isAir
}

/**
 * Allows items to till the ground.
 */
object Tilling : ItemBehavior {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val serverPlayer = player.serverPlayer
            val block = event.clickedBlock!!
            val level = block.world.serverLevel
            val pos = block.pos.nmsPos
            val interactionHand = event.hand!!.interactionHand
            
            val (check, newState, drops) = TILLABLES[block.nmsState.block] ?: return
            if (check.invoke(event)) {
                event.isCancelled = true
                
                // play sound
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1f, 1f)
                // update block
                level.setBlock(pos, newState, 11)
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(serverPlayer, newState))
                // drop items
                drops.forEach { Block.popResourceFromFace(level, pos, event.blockFace.nmsDirection, MojangStack(it)) }
                // damage item
                Damageable.damageAndBreak(serverPlayer.getItemInHand(interactionHand), 1, serverPlayer) { serverPlayer.broadcastBreakEvent(interactionHand) }
                // swing hand
                runTaskLater(1) { serverPlayer.swing(interactionHand, true) }
            }
        }
    }
    
}