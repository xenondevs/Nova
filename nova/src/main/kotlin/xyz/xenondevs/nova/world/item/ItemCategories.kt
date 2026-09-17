package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minecraft.core.registries.BuiltInRegistries
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.collections.mapValuesNotNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flattenIterables
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.orElseLazily
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.alias.ItemTypeEntry
import xyz.xenondevs.nova.registry.alias.ItemTypeEntrySet
import xyz.xenondevs.nova.registry.emptyRegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.serialization.kotlinx.ComponentAsMiniMessage
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrList
import xyz.xenondevs.nova.ui.menu.advancedTooltips
import xyz.xenondevs.nova.ui.menu.item.scrollableItemProvider
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.nmsItem
import java.util.*

internal object ItemCategories {
    
    val categories: Provider<List<ItemCategory>> = CONFIGS["nova:item_categories"]
        .optionalEntry<List<ItemCategory.Custom>>(emptyList())
        .orElseLazily { getDefaultItemCategories() }
    val obtainableItems: Provider<List<CategorizedItem>> = categories
        .flatMap { category -> combinedProvider(category.map(ItemCategory::categorizedItems)) }
        .flattenIterables()
    
    private fun getDefaultItemCategories(): List<ItemCategory> {
        val addonNamesById = AddonBootstrapper.addons.associate { it.namespace() to Component.text(it.name) }
        return Registry.ITEM
            .filter { it.isNova }
            .groupBy { it.key.namespace() }
            .mapValuesNotNull { [namespace, items] ->
                val name = addonNamesById[namespace]
                val visibleItems = items
                    .filterNot(ItemType::isHidden)
                    .sortedBy { BuiltInRegistries.ITEM.getId(it.nmsItem) } // sort by registration order
                if (name != null && visibleItems.isNotEmpty())
                    ItemCategory.Default(name, visibleItems)
                else null
            }
            .toSortedMap().values.toList()
    }
    
}

internal sealed interface ItemCategory {
    
    val iconProvider: Provider<ItemProvider>
    val categorizedItems: Provider<List<CategorizedItem>>
    
    @Serializable
    class Custom(
        val icon: ItemTypeEntry = ItemTypeEntries.AIR,
        val name: ComponentAsMiniMessage = Component.empty(),
        val description: ValueOrList<ComponentAsMiniMessage> = emptyList(),
        val items: ItemTypeEntrySet = emptyRegistryEntrySet(RegistryKey.ITEM)
    ) : ItemCategory {
        
        @Transient
        override val iconProvider = itemProvider(icon) {
            name by this@Custom.name
            lore by description
            data[DataComponentTypes.TOOLTIP_DISPLAY] by TooltipDisplay.tooltipDisplay()
                .hiddenComponents(Registry.DATA_COMPONENT_TYPE.toSet())
                .build()
            advancedTooltips by false
        }
        
        @Transient
        override val categorizedItems = items.mapEach { CategorizedItem(it.key, it.createItemStack(), it.name) }
        
    }
    
    class Default(
        private val name: Component,
        content: List<ItemType>
    ) : ItemCategory {
        override val iconProvider = itemProvider(content[0]) {
            name by this@Default.name
            lore by emptyList()
            advancedTooltips by false
        }
        override val categorizedItems = provider(content.map { CategorizedItem(it.key, it.createItemStack(), it.name) })
    }
    
}

internal class CategorizedItem(
    val key: Key,
    val itemStack: ItemStack,
    private val name: Component
) {
    
    private val nameCache = HashMap<Locale, String>()
    
    // cached here to avoid expensive recomputation
    val scrollableItemProvider = scrollableItemProvider(itemStack)
    
    fun getPlainTextName(locale: Locale): String =
        nameCache.getOrPut(locale) { name.toPlainText() }
    
}