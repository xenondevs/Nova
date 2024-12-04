package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

abstract class VerticalBar(
    private val height: Int,
    private val customModelDataIndex: Int = 0
) : Supplier<Item> {
    
    private var i = 0
    
    override fun get(): Item {
        i = (i - 1).mod(height)
        return createBarItem(i)
    }
    
    protected abstract fun createBarItem(section: Int): Item
    
    protected fun createItemBuilder(item: NovaItem, section: Int, percentage: Double): ItemBuilder {
        val displayPercentageStart = (1.0 / height) * section
        val displayPercentage = max(min((percentage - displayPercentageStart) * height, 1.0), 0.0)
        return item.createClientsideItemBuilder().setCustomModelData(customModelDataIndex, displayPercentage)
    }
    
}