@file:Suppress("unused")

package xyz.xenondevs.nova.event

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener

class PlayerDropItemSlotEvent internal constructor(
    player: Player,
    val droppedItem: ItemStack,
    val remainingItem: ItemStack?,
    val inventory: Inventory,
    val slot: Int
) : PlayerEvent(player) {
    
    override fun getHandlers(): HandlerList {
        return handlerList
    }
    
    @InternalInit(stage = InitializationStage.PRE_WORLD)
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
        
        @EventHandler(priority = EventPriority.HIGHEST)
        fun handleInventoryDrop(event: InventoryClickEvent) {
            if (event.whoClicked !is Player || event.currentItem == null) return
            val player = event.whoClicked as Player
            val item = event.currentItem!!
            
            if (event.click == ClickType.DROP) {
                val dropped = item.clone().apply { amount = 1 }
                val remaining = item.clone().apply { amount -= 1 }.takeUnlessEmpty()
                callEvent(PlayerDropItemSlotEvent(player, dropped, remaining, event.inventory, event.slot))
            } else if (event.click == ClickType.CONTROL_DROP) {
                callEvent(PlayerDropItemSlotEvent(player, item, null, event.inventory, event.slot))
            }
        }
        
        @PacketHandler
        fun handlePlayerAction(event: ServerboundPlayerActionPacketEvent) {
            val player = event.player
            val item = player.inventory.itemInMainHand.takeUnlessEmpty() ?: return
            if (event.action == ServerboundPlayerActionPacket.Action.DROP_ITEM) {
                val dropped = item.clone().apply { amount = 1 }
                val remaining = item.clone().apply { amount -= 1 }.takeUnlessEmpty()
                callEvent(PlayerDropItemSlotEvent(player, dropped, remaining, player.inventory, player.inventory.heldItemSlot))
            } else {
                callEvent(PlayerDropItemSlotEvent(player, item, null, player.inventory, player.inventory.heldItemSlot))
            }
        }
        
    }
    
}