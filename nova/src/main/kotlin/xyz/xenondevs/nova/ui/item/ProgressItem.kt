package xyz.xenondevs.nova.ui.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.item.NovaItem
import kotlin.math.roundToInt

open class ProgressItem(val item: NovaItem, private val maxState: Int) : AbstractItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            notifyWindows()
        }
    
    override fun getItemProvider(): ItemProvider {
        return item.model.createItemBuilder((percentage * maxState).roundToInt())
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
}
