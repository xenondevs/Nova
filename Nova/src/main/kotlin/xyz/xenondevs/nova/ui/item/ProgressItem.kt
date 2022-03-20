package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.ItemNovaMaterial
import kotlin.math.roundToInt

open class ProgressItem(val material: ItemNovaMaterial, private val maxState: Int) : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            notifyWindows()
        }
    
    override fun getItemProvider(): ItemProvider {
        return material.item.createItemBuilder("", (percentage * maxState).roundToInt())
    }
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
}
