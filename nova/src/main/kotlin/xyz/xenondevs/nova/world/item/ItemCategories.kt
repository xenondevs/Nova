package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.mapValuesNotNull
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatMapCollection
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.menu.itemProvider
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils

internal object ItemCategories {
    
    val categories: Provider<List<ItemCategory>> = Configs["nova:item_categories"]
        .map { it.get<List<ItemCategory>>()?.takeUnlessEmpty() }.orElseBy(getDefaultItemCategories())
    val obtainableItems: Provider<List<CategorizedItem>> = categories.flatMapCollection(ItemCategory::items)
    
    private fun getDefaultItemCategories(): Provider<List<ItemCategory>> =
        NovaRegistries.ITEM.entrySet.map { items ->
            items
                .groupBy { it.key.namespace() }
                .mapValuesNotNull { (namespace, items) ->
                    val categorizedItems = items.mapNotNull {
                        if (!it.isHidden) CategorizedItem(it.key.toString()) else null
                    }
                    if (categorizedItems.isNotEmpty()) {
                        val icon = itemProvider(items.first { !it.isHidden }) {
                            val addonName = AddonBootstrapper.addons
                                .firstOrNull { it.namespace() == namespace }
                                ?.name
                                ?: namespace
                            name by Component.text(addonName)
                            lore by emptyList()
                        }
                        ItemCategory(icon, categorizedItems)
                    } else null
                }
                .toSortedMap().values.toList()
        }
    
}

internal data class ItemCategory(val icon: Provider<ItemProvider>, val items: List<CategorizedItem>)

internal class CategorizedItem(val id: String) {
    
    val itemStack: ItemStack = ItemUtils.getItemStack(id)
    val name: Component = ItemUtils.getName(itemStack)
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is CategorizedItem && id == other.id
    }
    
}