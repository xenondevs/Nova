package xyz.xenondevs.nova.player.ability

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.config.Reloadable

abstract class Ability(val player: Player) : Reloadable {
    
    abstract fun handleRemove()
    
    abstract fun handleTick()
    
}