package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.item.impl.saveFilterConfig
import xyz.xenondevs.nova.material.NovaMaterialRegistry

class ItemFilter(
    var whitelist: Boolean,
    val items: Array<ItemStack?>
) {
    
    constructor(compound: CompoundElement) : 
        this(compound.getAsserted("whitelist"), compound.getAssertedElement<ListElement>("items").toTypedArray())
    
    val compound: CompoundElement
        get() = CompoundElement().also {
            val itemList = ListElement()
            items.forEach { itemStack -> itemList.add(itemStack) }
            it.put("items", itemList)
            it.put("whitelist", whitelist)
        }
    
    fun allowsItem(itemStack: ItemStack) =
        if (whitelist) items.any { it?.isSimilar(itemStack) ?: false }
        else items.none { it?.isSimilar(itemStack) ?: false }
    
    fun createFilterItem(): ItemStack {
        return NovaMaterialRegistry.ITEM_FILTER.createItemStack().apply { saveFilterConfig(this@ItemFilter) }
    }
    
}