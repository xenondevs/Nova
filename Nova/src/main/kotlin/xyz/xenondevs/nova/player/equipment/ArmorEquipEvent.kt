package xyz.xenondevs.nova.player.equipment

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

class ArmorEquipEvent(
    player: Player,
    val equipMethod: EquipMethod,
    val previousArmorItem: ItemStack?,
    val newArmorItem: ItemStack?,
    private val cancellable: Boolean = true
) : PlayerEvent(player), Cancellable {
    
    val equipAction = when {
        previousArmorItem == null && newArmorItem != null -> EquipAction.EQUIP
        previousArmorItem != null && newArmorItem == null -> EquipAction.UNEQUIP
        else -> EquipAction.CHANGE
    }
    
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

enum class EquipMethod {
    
    /**
     * When a player picks up a piece of armor and just puts in in the slot or just
     * takes armor out of the equipment slot normally.
     */
    NORMAL_CLICK,
    
    /**
     * When a player drops a piece of armor out of his armor equipment slots.
     */
    DROP,
    
    /**
     * When a player swaps the currently selected armor with a different piece.
     */
    SWAP,
    
    /**
     * When a player shift-clicks the armor piece into or out of the equipment slot.
     */
    SHIFT_CLICK,
    
    /**
     * When a player uses the shortcut keys to swap armor from the hotbar.
     */
    HOTBAR_SWAP,
    
    /**
     * When a player drags their cursor over the equipment slot and the armor piece
     * gets added there.
     */
    DRAG,
    
    /**
     * When a player right clicks with a piece of armor and equips it.
     */
    RIGHT_CLICK_EQUIP,
    
    /**
     * When a dispenser dispenses armor onto a player.
     */
    DISPENSER,
    
    /**
     * When the armor breaks.
     */
    BREAK,
    
    /**
     * When the player wearing the armor dies.
     */
    DEATH
    
}