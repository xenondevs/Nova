package xyz.xenondevs.nova.ui.menu.item.creative

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.impl.SimplePagedItemsGUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.impl.merged.split.AnvilSplitWindow
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.item.AnvilTextItem
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.openCorrespondingRecipesWindow
import xyz.xenondevs.nova.ui.overlay.CustomCharacters
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.searchFor

class ItemsWindow(val player: Player) : ItemMenu {
    
    private val mainGUI = GUIBuilder(GUIType.PAGED_ITEMS, 9, 6)
        .setStructure("" +
            "# # # < s > # # #" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x")
        .addIngredient('s', clickableItem(
            NovaMaterialRegistry.SEARCH_ICON
                .createBasicItemBuilder()
                .setLocalizedName(ChatColor.GRAY, "menu.nova.items.search-item"),
            ClickType.LEFT
        ) { openSearchWindow() })
        .build() as SimplePagedItemsGUI
    
    private val previewGUI = GUIBuilder(GUIType.PAGED_ITEMS, 9, 4)
        .setStructure("" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "# # # < # > # # s")
        .addIngredient('s', clickableItem(
            Icon.ARROW_1_UP.itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.back"),
            ClickType.LEFT
        ) { openMainWindow() })
        .build() as SimplePagedItemsGUI
    
    private val textItem = AnvilTextItem(NovaMaterialRegistry.INVISIBLE_ITEM.createBasicItemBuilder(), "")
    
    private val anvilSearchGUI = SimpleGUI(3, 1)
        .apply {
            setItem(0, textItem)
            setItem(2, clickableItem(
                Icon.X.itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.clear"),
                ClickType.LEFT
            ) { textItem.resetText(); filter = "" })
        }
    
    private var filteredItems: List<Item>? = null
    private var filter = ""
        set(value) {
            if (field != value && value != ".") {
                field = value
                updateFilteredItems()
            }
        }
    
    init {
        updateFilteredItems()
    }
    
    private fun updateFilteredItems() {
        val materials = NovaMaterialRegistry.sortedObtainables
        
        filteredItems = (if (filter.isEmpty()) materials
        else materials.searchFor(filter) { LocaleManager.getTranslatedName(player, it) })
            .sortedByDescending { it.isBlock }
            .map(::ItemsWindowDisplayItem)
        
        mainGUI.setItems(filteredItems)
        previewGUI.setItems(filteredItems)
    }
    
    private fun openMainWindow() {
        val builder = ComponentBuilder()
            .append(CustomCharacters.getMovingComponent(-8)) // move to side to place overlay
            .append(CustomCharacters.EMPTY_GUI.component)
            .append(CustomCharacters.getMovingComponent(-170)) // move back to start
        
        builder.append(
            if (filter == "") TranslatableComponent("menu.nova.items")
            else TranslatableComponent("menu.nova.items.searched").apply { addWith(coloredText(ChatColor.GRAY, filter)) }
        ).font("default").color(ChatColor.DARK_GRAY)
        
        SimpleWindow(player, builder.create(), mainGUI).show()
    }
    
    private fun openSearchWindow() {
        filter = ""
        
        val builder = ComponentBuilder()
            .append(CustomCharacters.getMovingComponent(-60)) // move to side to place overlay
            .append(CustomCharacters.SEARCH.component)
            .append(CustomCharacters.getMovingComponent(-170)) // move back to start
        
        builder.append(TranslatableComponent("menu.nova.items.search"))
            .font("default").color(ChatColor.DARK_GRAY)
        
        AnvilSplitWindow(
            player,
            builder.create(),
            anvilSearchGUI,
            previewGUI
        ) { filter = it }.show()
    }
    
    override fun show() {
        ItemMenu.addToHistory(player.uniqueId, this)
        openMainWindow()
    }
    
}

private class ItemsWindowDisplayItem(val material: NovaMaterial) : SimpleItem(material.itemProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
            openCorrespondingRecipesWindow(player, clickType, material.itemProvider)
        } else if (clickType == ClickType.MIDDLE && player.gameMode == GameMode.CREATIVE) {
            player.setItemOnCursor(material.createItemStack(material.maxStackSize))
        }
    }
    
}

fun clickableItem(provider: ItemProvider, vararg clickTypes: ClickType, run: (Player) -> Unit): Item {
    
    val clickTypesSet = clickTypes.toSet()
    
    return object : SimpleItem(provider) {
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickTypesSet.contains(clickType)) run(player)
        }
    }
    
}
