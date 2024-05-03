package xyz.xenondevs.nova.ui.menu.explorer.creative

import me.xdrop.fuzzywuzzy.FuzzySearch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.collections.weakHashSet
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
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.item.ItemCategories
import xyz.xenondevs.nova.item.ItemCategories.OBTAINABLE_ITEMS
import xyz.xenondevs.nova.item.ItemCategory
import xyz.xenondevs.nova.ui.menu.explorer.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.AnvilTextItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.ui.menu.item.ToggleItem
import xyz.xenondevs.nova.ui.menu.item.clickableItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.moveToStart
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.playClickSound

private val TAB_BUTTON_TEXTURES = arrayOf(
    DefaultGuiTextures.ITEMS_0,
    DefaultGuiTextures.ITEMS_1,
    DefaultGuiTextures.ITEMS_2,
    DefaultGuiTextures.ITEMS_3,
    DefaultGuiTextures.ITEMS_4
)

private const val GIVE_PERMISSION = "nova.command.give"

internal class ItemsWindow(val player: Player) : ItemMenu {
    
    private var currentWindow: Window? = null
    
    private val openSearchItem = clickableItem(
        DefaultGuiItems.TP_SEARCH.model.createClientsideItemBuilder()
            .setDisplayName(Component.translatable("menu.nova.items.search-item"))
    ) { openSearchWindow() }
    
    private val toggleCheatModeItem = ToggleItem(
        player in cheaters,
        DefaultGuiItems.TP_CHEATING_ON.model.createClientsideItemBuilder().setDisplayName(Component.translatable("menu.nova.items.cheat_mode.on")),
        DefaultGuiItems.TP_CHEATING_OFF.model.createClientsideItemBuilder().setDisplayName(Component.translatable("menu.nova.items.cheat_mode.off")),
    ) {
        if (player.hasPermission(GIVE_PERMISSION)) {
            player.persistentDataContainer.set(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN, it)
            if (it) cheaters += player else cheaters -= player
            return@ToggleItem true
        }
        return@ToggleItem false
    }
    
    private val openMainWindowItem = clickableItem(
        DefaultGuiItems.ARROW_UP_ON.model.createClientsideItemBuilder()
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
    
    private val textItem = AnvilTextItem(DefaultGuiItems.INVISIBLE_ITEM.model.createClientsideItemBuilder(), "")
    
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
            .map { (index, category) -> CreativeTabItem(index, category).apply { gui = mainGui } }
        tabPagesGui.setContent(tabButtons)
        
        updateFilteredItems()
        if (player.hasPermission(GIVE_PERMISSION)) {
            if (player !in cheaters && player.persistentDataContainer.get(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN) == true) {
                cheaters += player
                toggleCheatModeItem.state = true
                toggleCheatModeItem.notifyWindows()
            }
        } else {
            if (player in cheaters) {
                cheaters -= player
                toggleCheatModeItem.state = false
                toggleCheatModeItem.notifyWindows()
            }
        }
    }
    
    private fun handleTabPageChange(newTab: Int) {
        mainGui.setTab(newTab * 5)
        currentWindow?.changeTitle(getMainWindowTitle())
    }
    
    private fun updateFilteredItems() {
        filteredItems = if (filter.isNotEmpty()) {
            val names = OBTAINABLE_ITEMS
                .asSequence()
                .map { it to it.name.toPlainText(player) }
                .filter { (_, name) -> name.contains(filter, true) }
                .toMap(HashMap())
            val scores = FuzzySearch.extractAll(filter, names.values).associateTo(HashMap()) { it.string to it.score }
            names.keys.sortedWith { o1, o2 ->
                val s1 = scores[names[o1]]!!
                val s2 = scores[names[o2]]!!
                if (s1 == s2) {
                    val i1 = OBTAINABLE_ITEMS.indexOf(o1)
                    val i2 = OBTAINABLE_ITEMS.indexOf(o2)
                    i1.compareTo(i2)
                } else s1.compareTo(s2)
            }
        } else OBTAINABLE_ITEMS.toList()
        
        searchResultsGui.setContent(filteredItems)
        searchPreviewGui.setContent(filteredItems)
    }
    
    private fun getMainWindowTitle(): Component {
        return if (filter == "") {
            Component.text()
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
                "x x x x x x x x c",
                "x x x x x x x x u",
                "x x x x x x x x d"
            )
            .addIngredient('s', openSearchItem)
            .addIngredient('c', toggleCheatModeItem)
            .addIngredient('u', ScrollUpItem(
                on = DefaultGuiItems.TP_ARROW_UP_ON.model.clientsideProvider, 
                off = DefaultGuiItems.TP_ARROW_UP_OFF.model.clientsideProvider
            ))
            .addIngredient('d', ScrollDownItem(
                on = DefaultGuiItems.TP_ARROW_DOWN_ON.model.clientsideProvider, 
                off = DefaultGuiItems.TP_ARROW_DOWN_OFF.model.clientsideProvider
            ))
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
                DefaultGuiItems.TP_SMALL_ARROW_LEFT_ON.model.clientsideProvider
            else DefaultGuiItems.TP_SMALL_ARROW_LEFT_OFF.model.clientsideProvider
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
                DefaultGuiItems.TP_SMALL_ARROW_RIGHT_ON.model.clientsideProvider
            else DefaultGuiItems.TP_SMALL_ARROW_RIGHT_OFF.model.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            super.handleClick(clickType, player, event)
            player.playClickSound()
        }
        
    }
    
    companion object {
        val CHEAT_MODE_KEY = NamespacedKey(NOVA_PLUGIN, "cheat_mode")
        val cheaters = weakHashSet<Player>()
    }
    
}