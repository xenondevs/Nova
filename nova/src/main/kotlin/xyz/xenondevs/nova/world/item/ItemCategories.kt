package xyz.xenondevs.nova.world.item

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.CommentedConfigurationNode
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.addon.name
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.menu.explorer.creative.ItemsWindow
import xyz.xenondevs.nova.ui.menu.explorer.recipes.handleRecipeChoiceItemClick
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object ItemCategories {
    
    lateinit var CATEGORIES: List<ItemCategory>
        private set
    lateinit var OBTAINABLE_ITEMS: List<CategorizedItem>
        private set
    
    @InitFun
    private fun init() {
        val cfg = Configs["nova:item_categories"]
        reload(cfg.get())
        cfg.subscribe(::reload)
    }
    
    private fun reload(cfg: CommentedConfigurationNode) {
        CATEGORIES = cfg.get<List<ItemCategory>>()?.takeUnlessEmpty() ?: getDefaultItemCategories()
        OBTAINABLE_ITEMS = CATEGORIES.flatMap(ItemCategory::items)
    }
    
    private fun getDefaultItemCategories(): List<ItemCategory> =
        AddonBootstrapper.addons
            .sortedBy { it.name }
            .mapNotNull { addon ->
                NovaRegistries.ITEM
                    .filter { it.id.namespace == addon.id && !it.isHidden }
                    .takeUnlessEmpty()
                    ?.let { items ->
                        ItemCategory(
                            items[0].model.createClientsideItemBuilder(Component.text(addon.name)).setLore(emptyList()),
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
    
    override fun getItemProvider() = itemProvider
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (player in ItemsWindow.cheaters && player.hasPermission("nova.command.give")) {
            if (clickType == ClickType.MIDDLE) {
                player.setItemOnCursor(itemStack.clone().apply { amount = novaItem?.maxStackSize ?: type.maxStackSize })
            } else if (clickType.isShiftClick) {
                player.inventory.addItemCorrectly(itemStack)
            } else if (clickType == ClickType.NUMBER_KEY) {
                player.inventory.setItem(event.hotbarButton, itemStack)
            } else if (clickType.isMouseClick) {
                if (player.itemOnCursor.isSimilar(itemStack)) {
                    player.setItemOnCursor(player.itemOnCursor.apply { amount = (amount + 1).coerceAtMost(novaItem?.maxStackSize ?: maxStackSize) })
                } else {
                    player.setItemOnCursor(itemStack)
                }
            }
        } else {
            handleRecipeChoiceItemClick(player, clickType, event, itemProvider)
        }
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun equals(other: Any?): Boolean {
        return other is CategorizedItem && id == other.id
    }
    
}