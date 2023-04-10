package xyz.xenondevs.nova.ui.menu.item.creative

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.controlitem.PageItem
import xyz.xenondevs.invui.item.impl.controlitem.TabItem
import xyz.xenondevs.invui.window.AnvilWindow
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.changeTitle
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.item.ItemCategories
import xyz.xenondevs.nova.item.ItemCategory
import xyz.xenondevs.nova.ui.item.AnvilTextItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.overlay.character.gui.DefaultGuiTextures
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.searchFor

private val TAB_BUTTON_TEXTURES = arrayOf(
    DefaultGuiTextures.ITEMS_0,
    DefaultGuiTextures.ITEMS_1,
    DefaultGuiTextures.ITEMS_2,
    DefaultGuiTextures.ITEMS_3,
    DefaultGuiTextures.ITEMS_4
)

internal class ItemsWindow(val player: Player) : ItemMenu {
    
    private var currentWindow: Window? = null
    
    private val openSearchItem = clickableItem(
        DefaultGuiItems.TP_SEARCH.createClientsideItemBuilder()
            .setDisplayName(Component.translatable("menu.nova.items.search-item"))
    ) { openSearchWindow() }
    
    private val openMainWindowItem = clickableItem(
        DefaultGuiItems.ARROW_1_UP.createClientsideItemBuilder()
            .setDisplayName(Component.translatable("menu.nova.items.search.back", NamedTextColor.GRAY))
    ) { openMainWindow() }
    
    private val tabPagesGui = PagedGui.items()
        .setStructure(
            "x . x . x . x . x",
            "< . . . . . . . >"
        )
        .addIngredient('<', TabPageBackItem())
        .addIngredient('>', TabPageForwardItem())
        .addPageChangeHandler { _, now -> handleTabPageChange(now) }
        .build()
    
    private val mainGui = TabGui.normal()
        .setStructure(
            ". . . . . . . . .",
            ". . . . . . . . .",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        )
        .addIngredient('s', openSearchItem)
        .setTabs(ItemCategories.CATEGORIES.map(::createCategoryGui))
        .addModifier { it.fillRectangle(0, 0, tabPagesGui, true) }
        .build()
    
    private val searchResultsGui = PagedGui.items()
        .setStructure(
            "# # # < s > # # #",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        )
        .addIngredient('s', openSearchItem)
        .build()
    
    private val searchPreviewGui = PagedGui.items()
        .setStructure(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# # # < # > # # s"
        )
        .addIngredient('s', openMainWindowItem)
        .build()
    
    private val textItem = AnvilTextItem(DefaultGuiItems.INVISIBLE_ITEM.createClientsideItemBuilder(), "")
    
    private var filteredItems: List<Item>? = null
    private var filter = ""
        set(value) {
            if (field != value && value != ".") {
                field = value
                updateFilteredItems()
            }
        }
    
    init {
        val tabButtons = ItemCategories.CATEGORIES
            .withIndex()
            .map { (index, category) -> CreativeTabItem(index, category).apply { setGui(mainGui) } }
        tabPagesGui.setContent(tabButtons)
        
        updateFilteredItems()
    }
    
    private fun handleTabPageChange(newTab: Int) {
        mainGui.setTab(newTab * 5)
        currentWindow?.changeTitle(getMainWindowTitle())
    }
    
    private fun updateFilteredItems() {
        filteredItems = if (filter.isEmpty()) {
            ItemCategories.OBTAINABLE_ITEMS.toList()
        } else {
            ItemCategories.OBTAINABLE_ITEMS.searchFor(filter) {
                LocaleManager.getTranslation(player, it.localizedName)
            }
        }
        
        searchResultsGui.setContent(filteredItems)
        searchPreviewGui.setContent(filteredItems)
    }
    
    private fun getMainWindowTitle(): Component {
        return if (filter == "") {
            Component.text()
                .move(-8)
                .append(TAB_BUTTON_TEXTURES[mainGui.currentTab % 5].component)
                .build()
        } else {
            val title = Component.text()
                .append(Component.translatable("menu.nova.items"))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(filter, NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .build()
            
            DefaultGuiTextures.EMPTY_GUI.getTitle(title)
        }
    }
    
    private fun openMainWindow() {
        currentWindow = Window.single {
            it.setViewer(player)
            it.setTitle(getMainWindowTitle())
            it.setGui(if (filter == "") mainGui else searchResultsGui)
        }.apply { open() }
    }
    
    private fun openSearchWindow() {
        filter = ""
        
        val anvilGui = Gui.empty(3, 1).apply {
            setItem(0, textItem)
            setItem(2, clickableItem(
                DefaultGuiItems.X.createClientsideItemBuilder()
                    .setDisplayName(Component.translatable("menu.nova.items.search.clear", NamedTextColor.GRAY))
            ) { textItem.resetText(); filter = ""; runTask { player.updateInventory() } })
        }
        
        val title = Component.text()
            .move(-60)
            .append(DefaultGuiTextures.SEARCH.component)
            .moveToStart()
            .append(Component.translatable("menu.nova.items.search", NamedTextColor.DARK_GRAY))
            .build()
        
        currentWindow = AnvilWindow.split {
            it.setViewer(player)
            it.setTitle(title)
            it.setUpperGui(anvilGui)
            it.setLowerGui(searchPreviewGui)
            it.addRenameHandler { text -> filter = text }
        }.apply { open() }
    }
    
    override fun show() {
        ItemMenu.addToHistory(player.uniqueId, this)
        openMainWindow()
    }
    
    private fun createCategoryGui(category: ItemCategory): Gui {
        return ScrollGui.items()
            .setStructure(
                "x x x x x x x x s",
                "x x x x x x x x u",
                "x x x x x x x x .",
                "x x x x x x x x d"
            )
            .addIngredient('s', openSearchItem)
            .setContent(category.items)
            .build()
    }
    
    private inner class CreativeTabItem(private val tab: Int, private val category: ItemCategory) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGui) = category.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.currentTab != tab) {
                player.playClickSound()
                gui.setTab(tab)
                
                currentWindow?.changeTitle(getMainWindowTitle())
            }
        }
        
    }
    
    private class TabPageBackItem : PageItem(false) {
        
        override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasPreviousPage())
                DefaultGuiItems.TP_PIXEL_ARROW_LEFT_ON.clientsideProvider
            else DefaultGuiItems.TP_PIXEL_ARROW_LEFT_OFF.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
    private class TabPageForwardItem : PageItem(true) {
        
        override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasNextPage())
                DefaultGuiItems.TP_PIXEL_ARROW_RIGHT_ON.clientsideProvider
            else DefaultGuiItems.TP_PIXEL_ARROW_RIGHT_OFF.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
}