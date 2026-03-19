package xyz.xenondevs.nova.ui.menu

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData.customModelData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.dsl.ClickDsl
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.property.by
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.SlotElementSupplier
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

/**
 * A [SlotElementSupplier] for a vertical bar of [barType].
 * Slots provided to this supplier must be in a vertical line with no gaps, otherwise an [IllegalArgumentException] will be thrown.
 * Each item element supplied by this is a component of the vertical bar, which sections [percentage] into multiple parts.
 * Each part's sectioned percentage is applied to `customModelData.floats[0]` of [barType].
 * 
 * For example, if two slots are supplied, a [percentage] of `0.5` will result in the bottom
 * item's `customModelData.floats[0]` being set to `1.0` and the top item's `customModelData.floats[0]` being set to `0.0`. 
 */
fun verticalBar(
    percentage: Provider<Double>,
    barType: Provider<NovaItem>,
    modifyItemProvider: ItemProviderDsl.() -> Unit = {},
    onClick: ClickDsl.() -> Unit = {}
) = SlotElementSupplier { slots ->
    if (slots.isEmpty())
        return@SlotElementSupplier emptyList()
    
    val fromY = slots.minOf { it.y }
    val toY = slots.maxOf { it.y }
    val height = toY - fromY + 1
    
    require(slots.all { it.x == slots[0].x }) { "Slots must be in the same column" }
    require(toY - fromY + 1 == slots.size) { "Slots most be vertically continuous" }
    
    slots.map { slot ->
        val section = toY - slot.y
        item {
            itemProvider by itemProvider(barType) {
                this.data[DataComponentTypes.CUSTOM_MODEL_DATA] by percentage.map { percentage ->
                    val displayPercentageStart = (1.0 / height) * section
                    val displayPercentage = max(min((percentage - displayPercentageStart) * height, 1.0), 0.0)
                    customModelData().addFloat(displayPercentage.toFloat())
                }
                modifyItemProvider()
            }
            onClick(onClick)
        }.let(SlotElement::Item)
    }
}

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