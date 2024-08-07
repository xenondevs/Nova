package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.item.NovaItem
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

abstract class VerticalBar(private val height: Int) : Supplier<Item> {
    
    private var i = 0
    
    override fun get(): Item {
        i = (i - 1).mod(height)
        return createBarItem(i)
    }
    
    protected abstract fun createBarItem(section: Int): Item
    
    protected fun createItemBuilder(item: NovaItem, section: Int, percentage: Double): ItemBuilder {
        val displayPercentageStart = (1.0 / height) * section
        val displayPercentage = max(min((percentage - displayPercentageStart) * height, 1.0), 0.0)
        val state = round(displayPercentage * (item.model.size - 1)).toInt()
        return item.model.createClientsideItemBuilder(modelId = state)
    }
    
}