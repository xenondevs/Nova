package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatMapCollection
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils

internal object ItemCategories {
    
    val categories: Provider<List<ItemCategory>> = Configs["nova:item_categories"]
        .map { it.get<List<ItemCategory>>()?.takeUnlessEmpty() ?: getDefaultItemCategories() }
    val obtainableItems: Provider<List<CategorizedItem>> = categories.flatMapCollection(ItemCategory::items)
    
    private fun getDefaultItemCategories(): List<ItemCategory> =
        AddonBootstrapper.addons
            .sortedBy { it.name }
            .mapNotNull { addon ->
                NovaRegistries.ITEM
                    .filter { it.id.namespace() == addon.id && !it.isHidden }
                    .takeUnlessEmpty()
                    ?.let { items ->
                        ItemCategory(
                            items[0].createClientsideItemBuilder().setName(Component.text(addon.name)).setLore(emptyList()),
                            items.map { CategorizedItem(it.id.toString()) }
                        )
                    }
            }
    
}

internal data class ItemCategory(val icon: ItemProvider, val items: List<CategorizedItem>)

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