package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * An ui [Item] that changes its appearance based on a progress percentage.
 *
 * @param item The [NovaItem] that should be used for the progress item.
 * Its unnamed clientside providers will be used to display the progress.
 * @param customModelDataIndex The custom model data index that the progress value should be written to.
 */
open class ProgressItem(
    val item: NovaItem,
    private val customModelDataIndex: Int = 0
) : AbstractItem() {
    
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
    
    override fun getItemProvider(player: Player): ItemProvider {
        return item.createClientsideItemBuilder().setCustomModelData(customModelDataIndex, percentage)
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) = Unit
    
}
