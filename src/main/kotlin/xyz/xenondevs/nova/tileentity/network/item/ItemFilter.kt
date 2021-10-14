package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.item.impl.saveFilterConfig
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.novaMaterial

class ItemFilter(
    var whitelist: Boolean,
    var nbt: Boolean,
    var items: Array<ItemStack?>
) {
    
    constructor(compound: CompoundElement) :
        this(compound.getAsserted("whitelist"), compound.get("nbt")
            ?: false, compound.getAssertedElement<ListElement>("items").toTypedArray())
    
    constructor() : this(true, false, arrayOfNulls(7))
    
    val compound: CompoundElement
        get() = CompoundElement().also {
            val itemList = ListElement()
            items.forEach { itemStack -> itemList.add(itemStack) }
            it.put("items", itemList)
            it.put("nbt", nbt)
            it.put("whitelist", whitelist)
        }
    
    fun allowsItem(itemStack: ItemStack): Boolean {
        return if (whitelist) items.any { it?.checkFilterSimilarity(itemStack) ?: false }
        else items.none { it?.checkFilterSimilarity(itemStack) ?: false }
    }
    
    private fun ItemStack.checkFilterSimilarity(other: ItemStack): Boolean {
        return if (!nbt) {
            val novaMaterial = novaMaterial
            if (novaMaterial != null) novaMaterial == other.novaMaterial
            else type == other.type
        } else isSimilar(other)
    }
    
    fun createFilterItem(): ItemStack {
        return NovaMaterialRegistry.ITEM_FILTER.createItemStack().apply { saveFilterConfig(this@ItemFilter) }
    }
    
}