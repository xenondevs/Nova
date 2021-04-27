package xyz.xenondevs.nova.item

import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial

abstract class NovaItem(val material: NovaMaterial) {
    
    abstract fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent)
    
}