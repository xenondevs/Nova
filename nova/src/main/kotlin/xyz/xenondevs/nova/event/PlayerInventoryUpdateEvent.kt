@file:Suppress("unused")

package xyz.xenondevs.nova.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener

class PlayerInventoryUpdateEvent(
    player: Player,
    val slot: Int,
    val oldItemStack: ItemStack?,
    val newItemStack: ItemStack?
): PlayerEvent(player) {
    
    
    override fun getHandlers(): HandlerList {
        return handlerList
    }
    
    companion object : Listener {
    
        init {
            registerEvents()
            registerPacketListener()
        }
    
        @JvmStatic
        private val handlerList = HandlerList()
    
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }
    
}