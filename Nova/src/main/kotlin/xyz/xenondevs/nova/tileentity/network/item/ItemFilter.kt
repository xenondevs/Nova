package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.util.novaMaterial

private val ITEM_FILTER_KEY = NamespacedKey(NOVA, "itemFilterCBF")

fun ItemStack.getFilterConfigOrNull(): ItemFilter? {
    val container = itemMeta!!.persistentDataContainer
    return container.get(ITEM_FILTER_KEY, CompoundElementDataType)?.let(::ItemFilter)
}

fun ItemStack.getOrCreateFilterConfig(): ItemFilter = getFilterConfigOrNull() ?: ItemFilter()

fun ItemStack.saveFilterConfig(itemFilter: ItemFilter) {
    val itemMeta = itemMeta!!
    itemMeta.persistentDataContainer.set(ITEM_FILTER_KEY, CompoundElementDataType, itemFilter.compound)
    setItemMeta(itemMeta)
}

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
        return creatorFun(this)
    }
    
    companion object {
        lateinit var creatorFun: (ItemFilter) -> ItemStack
    }
    
}