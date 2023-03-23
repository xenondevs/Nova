package xyz.xenondevs.nova.player.equipment

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class ArmorEquipEvent(
    player: Player,
    val slot: EquipmentSlot,
    val action: EquipAction,
    val previous: ItemStack?,
    val now: ItemStack?,
    private val cancellable: Boolean = true
) : PlayerEvent(player), Cancellable {
    
    private var cancelled = false
    
    override fun isCancelled() = cancelled
    
    override fun setCancelled(cancel: Boolean) {
        if (!cancellable) throw IllegalStateException("This ArmorEquipEvent cannot be cancelled")
        this.cancelled = cancel
    }
    
    override fun getHandlers(): HandlerList {
        return handlerList
    }
    
    companion object {
        
        @JvmStatic
        private val handlerList = HandlerList()
        
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
        
    }
}

enum class EquipAction {
    
    /**
     * A player equipped a piece of armor.
     */
    EQUIP,
    
    /**
     * A player unequipped a piece of armor.
     */
    UNEQUIP,
    
    
    /**
     * A player replaced a piece of armor with another one.
     */
    CHANGE
    
}