package xyz.xenondevs.nova.item.behavior

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.gameevent.GameEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.item.damageItemInHand
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.particle.ParticleEffect
import kotlin.random.Random

private const val EXTINGUISH_CAMPFIRE_LEVEL_EVENT = 1009

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
                    if (state.getValue(CampfireBlock.SIGNAL_FIRE)) ParticleEffect.CAMPFIRE_SIGNAL_SMOKE else ParticleEffect.CAMPFIRE_COSY_SMOKE
                )
                
                val newState = state.setValue(CampfireBlock.LIT, false)
                level.setBlock(pos, newState, 11)
                level.levelEvent(null, EXTINGUISH_CAMPFIRE_LEVEL_EVENT, pos, 0)
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(serverPlayer, newState))
                
                val hand = event.hand!!
                player.damageItemInHand(hand)
                runTaskLater(1) { player.swingHand(hand) }
            }
        }
    }
    
    private fun displayCampfireExtinguishParticles(loc: Location, effect: ParticleEffect) {
        val players = Bukkit.getOnlinePlayers().filter { it.location.world == loc.world && it.location.distance(loc) <= 100 }
        repeat(20) {
            particleBuilder(effect) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                    0.5 + Random.nextDouble(-0.33, 0.33),
                ))
                offsetY(0.07f)
            }.display(players)
            
            particleBuilder(ParticleEffect.SMOKE_NORMAL) {
                location(loc.clone().add(
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                    0.5 + Random.nextDouble(-0.25, 0.25),
                ))
                offsetY(0.005f)
            }.display(players)
        }
    }
    
}