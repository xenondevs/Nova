package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.world.item.NovaItem
import kotlin.math.roundToInt

/**
 * An ui [Item] that changes its appearance based on a progress percentage.
 *
 * @param item The [NovaItem] that should be used for the progress item.
 * Its unnamed clientside providers will be used to display the progress.
 * @param maxState The maximum amount of states the item has.
 */
open class ProgressItem(val item: NovaItem, private val maxState: Int) : AbstractItem() {
    
    /**
     * The current progress percentage.
     *
     * Changing this value will update the item's appearance.
     *
     * Values are capped between 0..1.
     */
    var percentage: Double = 0.0
        set(value) {
            if (field == value)
                return
            
            field = value.coerceIn(0.0, 1.0)
            notifyWindows()
        }
    
    override fun getItemProvider(): ItemProvider {
        return item.model.unnamedClientsideProviders[(percentage * maxState).roundToInt()]
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
    
}
