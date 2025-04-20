package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatMapCollection
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.menu.explorer.creative.ItemsMenu
import xyz.xenondevs.nova.ui.menu.explorer.recipes.handleRecipeChoiceItemClick
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem

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

internal class CategorizedItem(val id: String) : AbstractItem() {
    
    private val itemStack: ItemStack = ItemUtils.getItemStack(id)
    private val itemProvider: ItemProvider = ItemWrapper(itemStack)
    val name: Component = ItemUtils.getName(itemStack)
    
    override fun getItemProvider(player: Player) = itemProvider
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (player.hasPermission("nova.command.give")
            && player.persistentDataContainer.get(ItemsMenu.CHEAT_MODE_KEY, PersistentDataType.BOOLEAN) == true
        ) {
            if (clickType == ClickType.MIDDLE) {
                player.setItemOnCursor(itemStack.clone().apply { amount = novaItem?.maxStackSize ?: type.maxStackSize })
            } else if (clickType.isShiftClick) {
                player.inventory.addItemCorrectly(itemStack)
            } else if (clickType == ClickType.NUMBER_KEY) {
                player.inventory.setItem(click.hotbarButton, itemStack)
            } else if (clickType.isMouseClick) {
                if (player.itemOnCursor.isSimilar(itemStack)) {
                    player.setItemOnCursor(player.itemOnCursor.apply { amount = (amount + 1).coerceAtMost(novaItem?.maxStackSize ?: maxStackSize) })
                } else {
                    player.setItemOnCursor(itemStack)
                }
            }
        } else {
            handleRecipeChoiceItemClick(this, click)
        }
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is CategorizedItem && id == other.id
    }
    
}