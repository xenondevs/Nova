package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.persistentdata.getLegacy
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter.Companion.ITEM_FILTER_KEY
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData

private val LEGACY_ITEM_FILTER_KEY = NamespacedKey(NOVA, "itemFilter")

fun ItemStack.getFilterConfigOrNull(): ItemFilter? {
    val itemMeta = itemMeta!!
    val container = itemMeta.persistentDataContainer
    
    val legacyFilter = container.getLegacy<ItemFilter>(LEGACY_ITEM_FILTER_KEY)
    if (legacyFilter != null) {
        container.remove(LEGACY_ITEM_FILTER_KEY)
        setItemMeta(itemMeta)
        saveFilterConfig(legacyFilter)
        return legacyFilter
    }
    
    return retrieveData(ITEM_FILTER_KEY)
}

fun ItemStack.getOrCreateFilterConfig(size: Int): ItemFilter = getFilterConfigOrNull() ?: ItemFilter(size)

fun ItemStack.saveFilterConfig(itemFilter: ItemFilter) {
    storeData(ITEM_FILTER_KEY, itemFilter)
}

class ItemFilter(
    var whitelist: Boolean,
    var nbt: Boolean,
    val size: Int,
    var items: Array<ItemStack?>
) {
    
    constructor(size: Int) : this(true, false, size, arrayOfNulls(size))
    
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
        val ITEM_FILTER_KEY = NamespacedId(NOVA, "itemfilter1")
        lateinit var creatorFun: (ItemFilter) -> ItemStack
    }
    
}