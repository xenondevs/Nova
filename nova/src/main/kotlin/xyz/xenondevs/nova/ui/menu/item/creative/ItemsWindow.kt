package xyz.xenondevs.nova.ui.menu.item.creative

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.PageItem
import xyz.xenondevs.invui.item.impl.controlitem.TabItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.builder.WindowType
import xyz.xenondevs.invui.window.type.create
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.ItemCategory
import xyz.xenondevs.nova.ui.item.AnvilTextItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.ui.overlay.character.gui.CoreGuiTexture
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.searchFor

private val TAB_BUTTON_TEXTURES = arrayOf(
    CoreGuiTexture.ITEMS_0,
    CoreGuiTexture.ITEMS_1,
    CoreGuiTexture.ITEMS_2,
    CoreGuiTexture.ITEMS_3,
    CoreGuiTexture.ITEMS_4
)

internal class ItemsWindow(val player: Player) : ItemMenu {
    
    private var currentWindow: Window? = null
    
    private val openSearchItem = clickableItem(
        CoreGuiMaterial.TP_SEARCH.createClientsideItemBuilder()
            .setLocalizedName("menu.nova.items.search-item")
    ) { openSearchWindow() }
    
    private val openMainWindowItem = clickableItem(
        CoreGuiMaterial.ARROW_1_UP.createClientsideItemBuilder()
            .setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.back")
    ) { openMainWindow() }
    
    private val tabPagesGui = GuiBuilder(GuiType.PAGED_ITEMS)
        .setStructure(
            "x . x . x . x . x",
            "< . . . . . . . >"
        )
        .addIngredient('<', TabPageBackItem())
        .addIngredient('>', TabPageForwardItem())
        .build().apply { registerPageChangeHandler(::handleTabPageChange) }
    
    private val mainGui = GuiBuilder(GuiType.TAB)
        .setStructure(
            ". . . . . . . . .",
            ". . . . . . . . .",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        )
        .addIngredient('s', openSearchItem)
        .setContent(ItemCategories.CATEGORIES.map(::createCategoryGui))
        .build().apply { fillRectangle(0, 0, tabPagesGui, true) }
    
    private val searchResultsGui = GuiBuilder(GuiType.PAGED_ITEMS)
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
    
    private val searchPreviewGui = GuiBuilder(GuiType.PAGED_ITEMS)
        .setStructure(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "# # # < # > # # s"
        )
        .addIngredient('s', openMainWindowItem)
        .build()
    
    private val textItem = AnvilTextItem(CoreGuiMaterial.INVISIBLE_ITEM.createClientsideItemBuilder(), "")
    
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
    
    private fun handleTabPageChange(previous: Int, now: Int) {
        mainGui.showTab(now * 5)
        currentWindow?.changeTitle(getMainWindowTitle())
    }
    
    private fun updateFilteredItems() {
        filteredItems = (if (filter.isEmpty()) ItemCategories.OBTAINABLE_ITEMS.toList()
        else ItemCategories.OBTAINABLE_ITEMS.searchFor(filter) { LocaleManager.getTranslation(player, it.localizedName) })
        
        searchResultsGui.setContent(filteredItems)
        searchPreviewGui.setContent(filteredItems)
    }
    
    private fun getMainWindowTitle(): Array<BaseComponent> {
        return if (filter == "") {
            ComponentBuilder()
                .append(MoveCharacters.getMovingComponent(-8)) // move to side to place overlay
                .append(TAB_BUTTON_TEXTURES[mainGui.currentTab % 5].component)
                .create()
        } else {
            val title = ComponentBuilder()
                .append(TranslatableComponent("menu.nova.items"))
                .append(" (").color(ChatColor.DARK_GRAY)
                .append(filter).color(ChatColor.GRAY)
                .append(")").color(ChatColor.DARK_GRAY)
                .create()
            
            CoreGuiTexture.EMPTY_Gui.getTitle(title)
        }
    }
    
    private fun openMainWindow() {
        currentWindow = WindowType.NORMAL.create {
            setViewer(player)
            setTitle(getMainWindowTitle())
            setGui(if (filter == "") mainGui else searchResultsGui)
        }.apply { show() }
    }
    
    private fun openSearchWindow() {
        filter = ""
        
        val anvilGui = Gui.empty(3, 1)
            .apply {
                setItem(0, textItem)
                setItem(2, clickableItem(
                    CoreGuiMaterial.X.createClientsideItemBuilder()
                        .setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.clear")
                ) { textItem.resetText(); filter = ""; runTask { player.updateInventory() } })
            }
        
        val builder = ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(-60)) // move to side to place overlay
            .append(CoreGuiTexture.SEARCH.component)
            .append(MoveCharacters.getMovingComponent(-170)) // move back to start
        
        builder.append(TranslatableComponent("menu.nova.items.search"))
            .font("default").color(ChatColor.DARK_GRAY)
        
        currentWindow = WindowType.ANVIL_SPLIT.create {
            setViewer(player)
            setTitle(builder.create())
            setUpperGui(anvilGui)
            setLowerGui(searchPreviewGui)
            setRenameHandler { filter = it }
        }.apply { show() }
    }
    
    override fun show() {
        ItemMenu.addToHistory(player.uniqueId, this)
        openMainWindow()
    }
    
    private fun createCategoryGui(category: ItemCategory): Gui {
        return GuiBuilder(GuiType.SCROLL_ITEMS)
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
                gui.showTab(tab)
                
                currentWindow?.changeTitle(getMainWindowTitle())
            }
        }
        
    }
    
    private class TabPageBackItem : PageItem(false) {
        
        override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasPreviousPage())
                CoreGuiMaterial.TP_PIXEL_ARROW_LEFT_ON.clientsideProvider
            else CoreGuiMaterial.TP_PIXEL_ARROW_LEFT_OFF.clientsideProvider
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
                CoreGuiMaterial.TP_PIXEL_ARROW_RIGHT_ON.clientsideProvider
            else CoreGuiMaterial.TP_PIXEL_ARROW_RIGHT_OFF.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
}