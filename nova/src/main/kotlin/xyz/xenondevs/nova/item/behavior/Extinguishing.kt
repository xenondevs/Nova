package xyz.xenondevs.nova.item.behavior

import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.gameevent.GameEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.util.item.damageItemInHand
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.sendTo
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random

private const val EXTINGuiSH_CAMPFIRE_LEVEL_EVENT = 1009

object Extinguishing : ItemBehavior() {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock!!
            val state = block.nmsState
            if (state.block == Blocks.CAMPFIRE && state.getValue(CampfireBlock.LIT)) {
                event.isCancelled = true
                
                val serverPlayer = player.serverPlayer
                val level = block.world.serverLevel
                val pos = block.pos.nmsPos
                
                CampfireBlock.dowse(player.serverPlayer, level, pos, state)
                displayCampfireExtinguishParticles(
                    block.location,
                    if (state.getValue(CampfireBlock.SIGNAL_FIRE)) ParticleTypes.CAMPFIRE_SIGNAL_SMOKE else ParticleTypes.CAMPFIRE_COSY_SMOKE
                )
                
                val newState = state.setValue(CampfireBlock.LIT, false)
                level.setBlock(pos, newState, 11)
                level.levelEvent(null, EXTINGuiSH_CAMPFIRE_LEVEL_EVENT, pos, 0)
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(serverPlayer, newState))
                
                val hand = event.hand!!
                player.damageItemInHand(hand)
                runTaskLater(1) { player.swingHand(hand) }
            }
        }
    }
    
    private fun displayCampfireExtinguishParticles(loc: Location, type: ParticleType<*>) {
        val players = Bukkit.getOnlinePlayers().filter { it.location.world == loc.world && it.location.distance(loc) <= 100 }
        repeat(20) {
            particle(type) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                ))
                offsetY(0.07f)
            }.sendTo(players)
            
            particle(ParticleTypes.SMOKE) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                ))
                offsetY(0.005f)
            }.sendTo(players)
        }
    }
    
}