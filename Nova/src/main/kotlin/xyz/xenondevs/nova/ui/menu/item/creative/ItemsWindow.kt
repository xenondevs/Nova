package xyz.xenondevs.nova.ui.menu.item.creative

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.impl.SimplePagedItemsGUI
import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.controlitem.PageItem
import de.studiocode.invui.item.impl.controlitem.TabItem
import de.studiocode.invui.window.Window
import de.studiocode.invui.window.impl.merged.split.AnvilSplitWindow
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.ItemCategory
import xyz.xenondevs.nova.ui.item.AnvilTextItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture
import xyz.xenondevs.nova.ui.overlay.MoveCharacters
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.searchFor

private val TAB_BUTTON_TEXTURES = arrayOf(
    CoreGUITexture.ITEMS_0,
    CoreGUITexture.ITEMS_1,
    CoreGUITexture.ITEMS_2,
    CoreGUITexture.ITEMS_3,
    CoreGUITexture.ITEMS_4
)

class ItemsWindow(val player: Player) : ItemMenu {
    
    private var currentWindow: Window? = null
    
    private val openSearchItem = clickableItem(
        CoreGUIMaterial.TP_SEARCH
            .createBasicItemBuilder()
            .setLocalizedName(ChatColor.GRAY, "menu.nova.items.search-item")
    ) { openSearchWindow() }
    
    private val openMainWindowItem = clickableItem(
        CoreGUIMaterial.ARROW_1_UP.createBasicItemBuilder().setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.back")
    ) { openMainWindow() }
    
    private val tabPagesGUI = GUIBuilder(GUIType.PAGED_ITEMS)
        .setStructure(
            "x . x . x . x . x",
            "< . . . . . . . >"
        )
        .addIngredient('<', TabPageBackItem())
        .addIngredient('>', TabPageForwardItem())
        .build().apply { addPageChangeHandler(::handleTabPageChange) }
    
    private val mainGUI = GUIBuilder(GUIType.TAB)
        .setStructure(
            ". . . . . . . . .",
            ". . . . . . . . .",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        )
        .addIngredient('s', openSearchItem)
        .setGUIs(ItemCategories.CATEGORIES.map(::createCategoryGUI))
        .build().apply { fillRectangle(0, 0, tabPagesGUI, true) }
    
    private val searchResultsGUI = GUIBuilder(GUIType.PAGED_ITEMS)
        .setStructure(
            "# # # < s > # # #",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        )
        .addIngredient('s', openSearchItem)
        .build() as SimplePagedItemsGUI
    
    private val searchPreviewGUI = GUIBuilder(GUIType.PAGED_ITEMS)
        .setStructure(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# # # < # > # # s"
        )
        .addIngredient('s', openMainWindowItem)
        .build() as SimplePagedItemsGUI
    
    private val textItem = AnvilTextItem(CoreGUIMaterial.INVISIBLE_ITEM.createBasicItemBuilder(), "")
    
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
            .map { (index, category) -> CreativeTabItem(index, category).apply { setGui(mainGUI) } }
        tabPagesGUI.setItems(tabButtons)
        
        updateFilteredItems()
    }
    
    private fun handleTabPageChange(previous: Int, now: Int) {
        mainGUI.showTab(now * 5)
        currentWindow?.changeTitle(getMainWindowTitle())
    }
    
    private fun updateFilteredItems() {
        filteredItems = (if (filter.isEmpty()) ItemCategories.OBTAINABLE_ITEMS.toList()
        else ItemCategories.OBTAINABLE_ITEMS.searchFor(filter) { LocaleManager.getTranslation(player, it.localizedName) })
        
        searchResultsGUI.setItems(filteredItems)
        searchPreviewGUI.setItems(filteredItems)
    }
    
    private fun getMainWindowTitle(): Array<BaseComponent> {
        return if (filter == "") {
            ComponentBuilder()
                .append(MoveCharacters.getMovingComponent(-8)) // move to side to place overlay
                .append(TAB_BUTTON_TEXTURES[mainGUI.currentTab % 5].component)
                .create()
        } else {
            val title = TranslatableComponent("menu.nova.items.searched")
            title.addWith(coloredText(ChatColor.GRAY, filter))
            CoreGUITexture.EMPTY_GUI.getTitle(title)
        }
    }
    
    private fun openMainWindow() {
        currentWindow = SimpleWindow(player, getMainWindowTitle(), if (filter == "") mainGUI else searchResultsGUI)
        currentWindow?.show()
    }
    
    private fun openSearchWindow() {
        filter = ""
        
        val anvilGUI = SimpleGUI(3, 1)
            .apply {
                setItem(0, textItem)
                setItem(2, clickableItem(
                    CoreGUIMaterial.X.createBasicItemBuilder().setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.clear")
                ) { textItem.resetText(); filter = ""; runTask { player.updateInventory() } })
            }
        
        val builder = ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(-60)) // move to side to place overlay
            .append(CoreGUITexture.SEARCH.component)
            .append(MoveCharacters.getMovingComponent(-170)) // move back to start
        
        builder.append(TranslatableComponent("menu.nova.items.search"))
            .font("default").color(ChatColor.DARK_GRAY)
        
        currentWindow = AnvilSplitWindow(
            player,
            builder.create(),
            anvilGUI,
            searchPreviewGUI
        ) { filter = it }
        currentWindow?.show()
    }
    
    override fun show() {
        ItemMenu.addToHistory(player.uniqueId, this)
        openMainWindow()
    }
    
    private fun createCategoryGUI(category: ItemCategory): GUI {
        return GUIBuilder(GUIType.SCROLL_ITEMS)
            .setStructure(
                "x x x x x x x x s",
                "x x x x x x x x u",
                "x x x x x x x x .",
                "x x x x x x x x d"
            )
            .addIngredient('s', openSearchItem)
            .setItems(category.items)
            .build()
    }
    
    private inner class CreativeTabItem(private val tab: Int, private val category: ItemCategory) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGUI) = category.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.currentTab != tab) {
                player.playClickSound()
                gui.showTab(tab)
                
                currentWindow?.changeTitle(getMainWindowTitle())
            }
        }
        
    }
    
    private class TabPageBackItem : PageItem(false) {
        
        override fun getItemProvider(gui: PagedGUI): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasPageBefore())
                CoreGUIMaterial.TP_PIXEL_ARROW_LEFT_ON.itemProvider
            else CoreGUIMaterial.TP_PIXEL_ARROW_LEFT_OFF.itemProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
    private class TabPageForwardItem : PageItem(true) {
        
        override fun getItemProvider(gui: PagedGUI): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasNextPage())
                CoreGUIMaterial.TP_PIXEL_ARROW_RIGHT_ON.itemProvider
            else CoreGUIMaterial.TP_PIXEL_ARROW_RIGHT_OFF.itemProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
}