package xyz.xenondevs.nova.ui.menu.item.creative

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.impl.SimplePagedItemsGUI
import de.studiocode.invui.gui.impl.TabGUI
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.controlitem.TabItem
import de.studiocode.invui.window.Window
import de.studiocode.invui.window.impl.merged.split.AnvilSplitWindow
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.ui.item.AnvilTextItem
import xyz.xenondevs.nova.ui.item.clickableItem
import xyz.xenondevs.nova.ui.menu.item.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.recipes.handleRecipeChoiceItemClick
import xyz.xenondevs.nova.ui.overlay.CoreGUITexture
import xyz.xenondevs.nova.ui.overlay.MoveCharacters
import xyz.xenondevs.nova.util.ItemUtils
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.getString
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.searchFor

private data class Category(val name: String, val icon: ItemProvider, val items: List<ObtainableItem>)

private val CATEGORIES: List<Category> = NovaConfig["creative_items"].getArray("categories")!!.map { obj ->
    obj as JsonObject
    val name = obj.getString("name")!!
    val icon = ItemUtils.getItemBuilder(obj.getString("icon")!!, true)
        .setDisplayName(TranslatableComponent(name))
        .get()
    val items = obj.getAsJsonArray("items").map { ObtainableItem(it.asString) }
    
    Category(name, ItemWrapper(icon), items)
}

private val OBTAINABLE_ITEMS: List<ObtainableItem> = CATEGORIES.flatMap { it.items }

private val TAB_BUTTON_TEXTURES = arrayOf(
    CoreGUITexture.ITEMS_0,
    CoreGUITexture.ITEMS_1,
    CoreGUITexture.ITEMS_2,
    CoreGUITexture.ITEMS_3,
    CoreGUITexture.ITEMS_4
)

class ObtainableItem(name: String) : BaseItem() {
    
    val localizedName: String
    private val itemStack: ItemStack
    private val itemWrapper: ItemWrapper
    
    init {
        val pair = ItemUtils.getItemAndLocalizedName(name)
        itemStack = pair.first
        localizedName = pair.second
        itemWrapper = ItemWrapper(itemStack)
    }
    
    override fun getItemProvider(): ItemProvider = itemWrapper
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        handleRecipeChoiceItemClick(player, clickType, event, itemWrapper)
    }
    
}

class ItemsWindow(val player: Player) : ItemMenu {
    
    private var currentWindow: Window? = null
    
    private val openSearchItem = clickableItem(
        NovaMaterialRegistry.SEARCH_ICON
            .createBasicItemBuilder()
            .setLocalizedName(ChatColor.GRAY, "menu.nova.items.search-item")
    ) { openSearchWindow() }
    
    private val openMainWindowItem = clickableItem(
        CoreGUIMaterial.ARROW_1_UP.createBasicItemBuilder().setLocalizedName(ChatColor.GRAY, "menu.nova.items.search.back")
    ) { openMainWindow() }
    
    private val mainGUI = GUIBuilder(GUIType.TAB, 9, 6)
        .setStructure("" +
            ". . . . . . . . ." +
            ". . . . . . . . ." +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x")
        .addIngredient('s', openSearchItem)
        .setGUIs(CATEGORIES.map(::createCategoryGUI))
        .build()
    
    private val searchResultsGUI = GUIBuilder(GUIType.PAGED_ITEMS, 9, 6)
        .setStructure("" +
            "# # # < s > # # #" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x")
        .addIngredient('s', openSearchItem)
        .build() as SimplePagedItemsGUI
    
    private val searchPreviewGUI = GUIBuilder(GUIType.PAGED_ITEMS, 9, 4)
        .setStructure("" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "x x x x x x x x x" +
            "# # # < # > # # s")
        .addIngredient('s', openMainWindowItem)
        .build() as SimplePagedItemsGUI
    
    private val textItem = AnvilTextItem(NovaMaterialRegistry.INVISIBLE_ITEM.createBasicItemBuilder(), "")
    
    private var filteredItems: List<Item>? = null
    private var filter = ""
        set(value) {
            if (field != value && value != ".") {
                field = value
                updateFilteredItems()
            }
        }
    
    init {
        CATEGORIES.withIndex().forEach { (index, category) ->
            mainGUI.setItem(index * 2, CreativeTabItem(index, category))
        }
        
        updateFilteredItems()
    }
    
    private fun updateFilteredItems() {
        filteredItems = (if (filter.isEmpty()) OBTAINABLE_ITEMS
        else OBTAINABLE_ITEMS.searchFor(filter) { LocaleManager.getTranslation(player, it.localizedName) })
        
        searchResultsGUI.setItems(filteredItems)
        searchPreviewGUI.setItems(filteredItems)
    }
    
    private fun getMainWindowTitle(): Array<BaseComponent> {
        return if (filter == "") {
            ComponentBuilder()
                .append(MoveCharacters.getMovingComponent(-8)) // move to side to place overlay
                .append(TAB_BUTTON_TEXTURES[mainGUI.currentTab].component)
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
    
    private fun createCategoryGUI(category: Category): GUI {
        return GUIBuilder(GUIType.SCROLL_ITEMS, 9, 4)
            .setStructure("" +
                "x x x x x x x x s" +
                "x x x x x x x x u" +
                "x x x x x x x x ." +
                "x x x x x x x x d")
            .addIngredient('s', openSearchItem)
            .setItems(category.items)
            .build()
    }
    
    private inner class CreativeTabItem(private val tab: Int, private val category: Category) : TabItem(tab) {
        
        override fun getItemProvider(gui: TabGUI) = category.icon
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.currentTab != tab) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                gui.showTab(tab)
                
                currentWindow?.changeTitle(getMainWindowTitle())
            }
        }
        
    }
    
}