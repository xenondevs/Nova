package xyz.xenondevs.nova.player.ability

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.JETPACK
import xyz.xenondevs.nova.ui.overlay.ActionbarOverlayManager
import xyz.xenondevs.nova.ui.overlay.impl.JetpackOverlay
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.particle.ParticleEffect

private val ENERGY_PER_TICK = NovaConfig[JETPACK].getLong("energy_per_tick")!!
private val FLY_SPEED = NovaConfig[JETPACK].getFloat("fly_speed")!!

internal class JetpackFlyAbility(player: Player) : Ability(player) {
    
    private val wasFlying = player.isFlying
    private val wasAllowFlight = player.allowFlight
    private val previousFlySpeed = player.flySpeed
    
    private val overlay = JetpackOverlay()
    private val jetpackItem: ItemStack?
        get() = player.equipment?.chestplate
    
    init {
        player.isFlying = false
        player.flySpeed = FLY_SPEED
        
        ActionbarOverlayManager.registerOverlay(player, overlay)
    }
    
    override fun handleRemove() {
        player.allowFlight = wasAllowFlight
        player.isFlying = wasFlying
        player.flySpeed = previousFlySpeed
        
        ActionbarOverlayManager.unregisterOverlay(player, overlay)
    }
    
    override fun handleTick(tick: Int) {
        val jetpackItem = jetpackItem
        
        if (jetpackItem != null) {
            val energyLeft = JetpackItem.getEnergy(jetpackItem)
            overlay.percentage = energyLeft / JetpackItem.maxEnergy.toDouble()
            
            if (energyLeft > ENERGY_PER_TICK) {
                if (player.isFlying) {
                    JetpackItem.addEnergy(jetpackItem, -ENERGY_PER_TICK)
                    if (tick % 3 == 0) {
                        val location = player.location
                        playSound(location)
                        spawnParticles(location)
                    }
                } else {
                    player.allowFlight = true
                }
            } else if (player.isFlying) {
                player.isFlying = false
                player.allowFlight = false
            }
        } else JetpackItem.setJetpack(player, false)
    }
    
    private fun playSound(location: Location) {
        location.world!!.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.2f, 5f)
    }
    
    private fun spawnParticles(location: Location) {
        location.pitch = 0f
        location.y += 0.5
        location.yaw -= 160
        spawnParticle(location.clone().add(location.direction.multiply(0.5)))
        location.yaw -= 70
        spawnParticle(location.clone().add(location.direction.multiply(0.5)))
    }
    
    private fun spawnParticle(location: Location) {
        particleBuilder(ParticleEffect.FLAME, location) {
            offsetY(-0.5f)
        }.display()
    }
    
}