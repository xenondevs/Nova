package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.Observer
import xyz.xenondevs.invui.internal.util.InventoryUtils
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.item.isNullOrEmpty
import xyz.xenondevs.nova.world.player.swingMainHandEventless

/**
 * UI item that acts as if there were no slot, letting the player drop items by clicking on it.
 */
internal object NoSlotItem : Item {
    
    override fun getItemProvider(viewer: Player) = ItemProvider.EMPTY
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        val cursor = player.itemOnCursor
        if (cursor.isNullOrEmpty())
            return
        
        when (clickType) {
            ClickType.LEFT -> {
                InventoryUtils.dropItemLikePlayer(player, cursor)
                player.swingMainHandEventless()
                player.setItemOnCursor(null)
            }
            ClickType.RIGHT -> {
                InventoryUtils.dropItemLikePlayer(player, cursor.clone().apply { amount = 1 })
                player.swingMainHandEventless()
                player.setItemOnCursor(player.itemOnCursor.apply { amount-- })
            }
            else -> Unit
        }
    }
    
    override fun notifyWindows() = Unit
    override fun addObserver(who: Observer, what: Int, how: Int) = Unit
    override fun removeObserver(who: Observer, what: Int, how: Int) = Unit
    
}