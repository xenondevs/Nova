package xyz.xenondevs.nova.world.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.mapValuesNotNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flattenIterables
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.emptyRegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.mapEach
import xyz.xenondevs.nova.serialization.kotlinx.ComponentAsMiniMessage
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrList
import xyz.xenondevs.nova.ui.menu.item.scrollableItemProvider
import xyz.xenondevs.nova.ui.menu.itemProvider
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import java.util.*

internal object ItemCategories {
    
    val categories: Provider<List<ItemCategory>> = CONFIGS["nova:item_categories"]
        .optionalEntry<List<@Contextual ItemCategory.Custom>>(emptyList())
        .orElseBy(getDefaultItemCategories())
    val obtainableItems: Provider<List<CategorizedItem>> = categories
        .flatMap { category -> combinedProvider(category.map(ItemCategory::categorizedItems)) }
        .flattenIterables()
    
    private fun getDefaultItemCategories(): Provider<List<ItemCategory>> {
        val addonNamesById = AddonBootstrapper.addons.associate { it.namespace() to Component.text(it.name) }
        return NovaRegistries.ITEM.entrySet.map { items ->
            items
                .groupBy { it.key.namespace() }
                .mapValuesNotNull { (namespace, items) ->
                    val name = addonNamesById[namespace]
                    val visibleItems = items.filterNot(NovaItem::isHidden)
                    if (name != null && visibleItems.isNotEmpty())
                        ItemCategory.Default(name, visibleItems)
                    else null
                }
                .toSortedMap().values.toList()
        }
    }
    
}

internal sealed interface ItemCategory {
    
    val iconProvider: Provider<ItemProvider>
    val categorizedItems: Provider<List<CategorizedItem>>
    
    @Serializable
    class Custom(
        val icon: EitherItemTypeEntry = ItemTypeEntries.AIR.asEither(),
        val name: ComponentAsMiniMessage = Component.empty(),
        val description: ValueOrList<ComponentAsMiniMessage> = emptyList(),
        val items: MixedItemTypeEntrySet = emptyRegistryEntrySet(NovaRegistries.ITEM, RegistryKey.ITEM)
    ) : ItemCategory {
        
        @Transient
        override val iconProvider = itemProvider(icon) {
            name by this@Custom.name
            lore by description
            data[DataComponentTypes.TOOLTIP_DISPLAY] by TooltipDisplay.tooltipDisplay()
                .hiddenComponents(Registry.DATA_COMPONENT_TYPE.toSet())
                .build()
        }
        
        @Transient
        override val categorizedItems = items.mapEach(
            { CategorizedItem(it.key, it.createItemStack(), it.name ?: Component.empty()) },
            { CategorizedItem(it.key, it.createItemStack(), it.getDefaultData(DataComponentTypes.ITEM_NAME) ?: Component.empty()) }
        )
        
    }
    
    class Default(
        private val name: Component,
        content: List<NovaItem>
    ) : ItemCategory {
        override val iconProvider = itemProvider(content[0]) { 
            name by this@Default.name
            lore by emptyList()
        }
        override val categorizedItems = provider(content.map { CategorizedItem(it.key, it.createItemStack(), it.name ?: Component.empty()) })
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