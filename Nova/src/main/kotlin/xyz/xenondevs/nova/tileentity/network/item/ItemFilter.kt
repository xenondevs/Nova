package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.util.item.novaMaterial

private val ITEM_FILTER_KEY = NamespacedKey(NOVA, "itemFilter")

fun ItemStack.getFilterConfigOrNull(): ItemFilter? {
    val container = itemMeta!!.persistentDataContainer
    return container.get<LegacyCompound>(ITEM_FILTER_KEY)?.let(::ItemFilter)
}

fun ItemStack.getOrCreateFilterConfig(size: Int): ItemFilter = getFilterConfigOrNull() ?: ItemFilter(size)

fun ItemStack.saveFilterConfig(itemFilter: ItemFilter) {
    val itemMeta = itemMeta!!
    
    itemMeta.persistentDataContainer.set(ITEM_FILTER_KEY, itemFilter.compound)
    setItemMeta(itemMeta)
}

fun ItemFilter(compound: LegacyCompound): ItemFilter {
    val items: Array<ItemStack?> = compound.get<List<ItemStack>>("items")!!.toTypedArray()
    return ItemFilter(
        compound["whitelist"]!!,
        compound["nbt"] ?: false,
        items.size,
        items
    )
}

class ItemFilter(
    var whitelist: Boolean,
    var nbt: Boolean,
    val size: Int,
    var items: Array<ItemStack?>
) {
    
    constructor(size: Int) : this(true, false, size, arrayOfNulls(size))
    
    val compound: Compound
        get() = Compound().also {
            val itemList = items.toList()
            it["items"] = itemList
            it["nbt"] = nbt
            it["whitelist"] = whitelist
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