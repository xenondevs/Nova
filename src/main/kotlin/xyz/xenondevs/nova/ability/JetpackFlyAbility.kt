package xyz.xenondevs.nova.ability

import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.ability.AbilityManager.AbilityType
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.particle.ParticleEffect

private val ENERGY_PER_TICK = NovaConfig.getInt("jetpack.energy_per_tick")!!
private val FLY_SPEED = NovaConfig.getFloat("jetpack.fly_speed")!!

internal class JetpackFlyAbility(player: Player) : Ability(player) {
    
    private val wasFlying = player.isFlying
    private val wasAllowFlight = player.allowFlight
    private val previousFlySpeed = player.flySpeed
    
    private lateinit var jetpackItem: ItemStack
    
    init {
        player.isFlying = false
        player.flySpeed = FLY_SPEED
    }
    
    override fun handleRemove() {
        player.allowFlight = wasAllowFlight
        player.isFlying = wasFlying
        player.flySpeed = previousFlySpeed
    }
    
    override fun handleTick(tick: Int) {
        if (!::jetpackItem.isInitialized) {
            if (player.equipment?.chestplate == null) {
                AbilityManager.takeAbility(player, AbilityType.JETPACK)
                return
            }
            jetpackItem = player.equipment!!.chestplate!!
        }
        
        val energyLeft = JetpackItem.getEnergy(jetpackItem)
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