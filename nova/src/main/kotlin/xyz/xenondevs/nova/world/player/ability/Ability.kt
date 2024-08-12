package xyz.xenondevs.nova.world.player.ability

import org.bukkit.entity.Player

abstract class Ability(val player: Player) {
    
    abstract fun handleRemove()
    
    abstract fun handleTick()
    
}