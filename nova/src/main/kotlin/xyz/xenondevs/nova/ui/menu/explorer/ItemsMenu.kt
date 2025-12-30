package xyz.xenondevs.nova.ui.menu.explorer

import io.papermc.paper.datacomponent.DataComponentTypes
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mapEachIndexed
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.plus
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.dsl.anvilWindow
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.mergedWindow
import xyz.xenondevs.invui.dsl.scrollItemsGui
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.SlotElement.InventoryLink
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.invui.inventory.ReferencingInventory
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.invui.inventory.get
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.ui.menu.explorer.recipes.RecipesMenu
import xyz.xenondevs.nova.ui.menu.explorer.recipes.handleRecipeChoiceClick
import xyz.xenondevs.nova.ui.menu.item.NoSlotItem
import xyz.xenondevs.nova.ui.menu.item.scrollDownItem
import xyz.xenondevs.nova.ui.menu.item.scrollLeftItem
import xyz.xenondevs.nova.ui.menu.item.scrollRightItem
import xyz.xenondevs.nova.ui.menu.item.scrollUpItem
import xyz.xenondevs.nova.ui.menu.item.scrollerItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.CategorizedItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.ItemCategories
import xyz.xenondevs.nova.world.item.ItemCategory

private const val CATEGORY_TAB_COUNT = 8
private val TAB_BUTTON_TEXTURES = arrayOf(
    DefaultGuiTextures.ITEMS_0,
    DefaultGuiTextures.ITEMS_1,
    DefaultGuiTextures.ITEMS_2,
    DefaultGuiTextures.ITEMS_3,
    DefaultGuiTextures.ITEMS_5,
    DefaultGuiTextures.ITEMS_6,
    DefaultGuiTextures.ITEMS_7,
    DefaultGuiTextures.ITEMS_8
)

private const val GIVE_PERMISSION = "nova.command.give"
private val CHEAT_MODE_KEY = NamespacedKey("nova", "cheat_mode")

internal class ItemsMenu private constructor(val player: Player) {
    
    private val mainWindow: Provider<Window>
    private val searchWindow: Provider<Window>
    private val searchResultsWindow: Provider<Window>
    
    private var activeWindow: Provider<Window>
    
    init {
        val tab: MutableProvider<Int> = mutableProvider(0)
        val playerInvTab = ItemCategories.categories.map { it.size } // player inv tab is last tab
        val filter: MutableProvider<String> = mutableProvider("")
        val cheatMode: MutableProvider<Boolean> = mutableProvider { player.persistentDataContainer.get(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN) == true }
        cheatMode.subscribe { cheatMode -> player.persistentDataContainer.set(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN, cheatMode) }
        
        val filteredItems: Provider<List<Item>> = combinedProvider(filter, ItemCategories.obtainableItems)
            .map { (filter, obtainableItems) -> filterItems(player, filter, obtainableItems) }
            .mapEach { i -> categorizedItemButton(i, cheatMode) }
        
        mainWindow = provider {
            mergedWindow(player) {
                title by combinedProvider(tab, playerInvTab) { tab, playerInvTab ->
                    if (tab < playerInvTab)
                        TAB_BUTTON_TEXTURES[(tab % CATEGORY_TAB_COUNT).coerceAtLeast(0)].component
                    else DefaultGuiTextures.ITEMS_9.component
                }
                gui by tabGui(
                    "p * p * p * p * s",
                    "< . . . . . . . >",
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "p * p * p * p * i",
                    "* * * * * * * * *",
                    "* * * * * * * * *",
                    "h h h h h h h h h"
                ) {
                    val tabButtonsPage = mutableProvider(0)
                    val tabButtonsPageCount = mutableProvider(0)
                    '<' by tabPageBackItem(tabButtonsPage, tabButtonsPageCount)
                    '>' by tabPageForwardItem(tabButtonsPage, tabButtonsPageCount)
                    's' by searchButton(searchWindow)
                    'i' by playerInventoryTabButton(ItemCategories.categories.map { it.size }, tab) // last tab
                    'h' by ReferencingInventory.fromPlayerStorageContents(player.inventory)[27..35].apply {
                        setGuiPriority(Integer.MAX_VALUE)
                    }
                    '*' by NoSlotItem
                    'c' by cheatModeButton(cheatMode)
                    
                    'p' by pagedItemsGui(
                        "x . x . x . x . .",
                        ". . . . . . . . .",
                        ". . . . . . . . .",
                        ". . . . . . . . .",
                        ". . . . . . . . .",
                        ". . . . . . . . .",
                        "x . x . x . x . ."
                    ) {
                        page by tabButtonsPage
                        page.subscribe {
                            // adjust tab if not currently looking at player inventory tab
                            if (tab.get() != playerInvTab.get())
                                tab.set(it * CATEGORY_TAB_COUNT)
                        }
                        tabButtonsPageCount.consume(pageCount)
                        content by ItemCategories.categories.mapEachIndexed { i, cat -> itemCategoryTabButton(i, cat, tab) }
                    }
                    
                    this.tab by tab
                    
                    tabs by ItemCategories.categories.mapEach { category ->
                        scrollItemsGui(
                            ". x x x x x x x c",
                            ". x x x x x x x u",
                            ". x x x x x x x |",
                            ". x x x x x x x d"
                        ) {
                            'c' by cheatModeButton(cheatMode)
                            'u' by scrollUpItem(line)
                            '|' by scrollerItem(serverWindowState, clientWindowState, line, maxLine)
                            'd' by scrollDownItem(line, maxLine)
                            content by category.items.map { i -> categorizedItemButton(i, cheatMode) }
                        }
                    } + gui(
                        ". x x x x x x x h",
                        ". x x x x x x x c",
                        ". x x x x x x x l",
                        ". x x x x x x o b"
                    ) {
                        val inv = ReferencingInventory.fromContents(player.inventory)
                        'h' by InventoryLink(inv[39..39].apply { allowOnlyEquippable(EquipmentSlot.HEAD) }, 0, DefaultGuiItems.HEAD_PLACEHOLDER.clientsideProvider)
                        'c' by InventoryLink(inv[38..38].apply { allowOnlyEquippable(EquipmentSlot.CHEST) }, 0, DefaultGuiItems.CHEST_PLACEHOLDER.clientsideProvider)
                        'l' by InventoryLink(inv[37..37].apply { allowOnlyEquippable(EquipmentSlot.LEGS) }, 0, DefaultGuiItems.LEGS_PLACEHOLDER.clientsideProvider)
                        'b' by InventoryLink(inv[36..36].apply { allowOnlyEquippable(EquipmentSlot.FEET) }, 0, DefaultGuiItems.FEET_PLACEHOLDER.clientsideProvider)
                        'o' by InventoryLink(inv, 40, DefaultGuiItems.OFF_HAND_PLACEHOLDER.clientsideProvider)
                        'x' by ReferencingInventory.fromPlayerStorageContents(player.inventory)[0..26]
                    }
                }
                
                onClose { player.updateInventory() } // for armor slots
                onOpen { activeWindow = mainWindow }
            }
        }
        
        searchWindow = provider {
            anvilWindow(player) {
                title by DefaultGuiTextures.SEARCH.component
                lowerGui by scrollItemsGui(
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "x x x x x x x x x",
                    "c . . < - > . . ^"
                ) {
                    'x' by Markers.CONTENT_LIST_SLOT_VERTICAL
                    'c' by cheatModeButton(cheatMode)
                    '<' by scrollLeftItem(line)
                    '-' by scrollerItem(serverWindowState, clientWindowState, line, maxLine, provider(DefaultGuiItems.TP_SCROLLER_HORIZONTAL.clientsideProvider))
                    '>' by scrollRightItem(line, maxLine)
                    '^' by closeSearchButton(filter, mainWindow, searchResultsWindow)
                    content by filteredItems
                }
                text.subscribe(filter::set)
                
                fallbackWindow by text.flatMap { if (it.isBlank()) mainWindow else searchResultsWindow }
                onOpen { activeWindow = searchWindow }
                onOutsideClick { isCancelled = true }
            }
        }
        
        searchResultsWindow = provider {
            window(player) {
                title by filter.map { filter ->
                    val title = Component.text()
                        .append(Component.translatable("menu.nova.items"))
                        .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                        .append(Component.text(filter, NamedTextColor.GRAY))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                        .build()
                    DefaultGuiTextures.SEARCH_RESULTS.getTitle(title)
                }
                
                upperGui by scrollItemsGui(
                    "x x x x x x x x c",
                    "x x x x x x x x s",
                    "x x x x x x x x u",
                    "x x x x x x x x |",
                    "x x x x x x x x d",
                    "x x x x x x x x ."
                ) {
                    's' by searchButton(searchWindow)
                    'c' by cheatModeButton(cheatMode)
                    'u' by scrollUpItem(line)
                    '|' by scrollerItem(serverWindowState, clientWindowState, line, maxLine)
                    'd' by scrollDownItem(line, maxLine)
                    content by filteredItems
                }
                
                fallbackWindow by mainWindow
                onOpen { activeWindow = searchResultsWindow }
            }
        }
        
        activeWindow = mainWindow
    }
    
    fun open() {
        RecipesMenu.clearHistory(player)
        activeWindow.get().open()
    }
    
    companion object {
        
        private val menus: MutableMap<Player, ItemsMenu> = PlayerMapManager.createMap()
        
        fun open(player: Player) =
            menus.computeIfAbsent(player, ::ItemsMenu).open()
    }
    
}

private fun filterItems(player: Player, filter: String, obtainableItems: List<CategorizedItem>): List<CategorizedItem> {
    if (filter.isEmpty())
        return obtainableItems
    
    val names = obtainableItems
        .asSequence()
        .map { it to it.name.toPlainText(player) }
        .filter { (_, name) -> name.contains(filter, true) }
        .toMap(HashMap())
    val scores = FuzzySearch.extractAll(filter, names.values).associateTo(HashMap()) { it.string to it.score }
    return names.keys.sortedWith { o1, o2 ->
        val s1 = scores[names[o1]]!!
        val s2 = scores[names[o2]]!!
        if (s1 == s2) {
            val i1 = obtainableItems.indexOf(o1)
            val i2 = obtainableItems.indexOf(o2)
            i1.compareTo(i2)
        } else s1.compareTo(s2)
    }
}

private fun categorizedItemButton(item: CategorizedItem, cheatMode: Provider<Boolean>) = item {
    val itemStack = item.itemStack
    
    itemProvider by itemStack
    onClick {
        if (player.hasPermission(GIVE_PERMISSION) && cheatMode.get()) {
            when {
                clickType == ClickType.MIDDLE -> player.setItemOnCursor(itemStack.clone().apply { amount = novaItem?.maxStackSize ?: type.maxStackSize })
                clickType == ClickType.NUMBER_KEY -> player.inventory.setItem(hotbarButton, itemStack)
                clickType.isShiftClick -> player.inventory.addItemCorrectly(itemStack)
                clickType.isMouseClick -> {
                    if (player.itemOnCursor.isSimilar(itemStack)) {
                        player.setItemOnCursor(player.itemOnCursor.apply { amount = (amount + 1).coerceAtMost(novaItem?.maxStackSize ?: maxStackSize) })
                    } else {
                        player.setItemOnCursor(itemStack)
                    }
                }
            }
        } else {
            handleRecipeChoiceClick(item.id, Click(player, clickType, hotbarButton))
        }
    }
}

private fun closeSearchButton(filter: Provider<String>, main: Provider<Window>, results: Provider<Window>) = item {
    itemProvider by DefaultGuiItems.TP_ARROW_UP_ON.createClientsideItemBuilder()
        .setName(Component.translatable("menu.nova.items.search.back", NamedTextColor.GRAY))
    onClick {
        if (clickType.isLeftClick) {
            if (filter.get().isBlank())
                main.get().open()
            else results.get().open()
            player.playClickSound()
        }
    }
}

private fun searchButton(search: Provider<Window>) = item {
    itemProvider by DefaultGuiItems.TP_SEARCH.clientsideProvider
    onClick {
        if (clickType.isLeftClick) {
            search.get().open()
            player.playClickSound()
        }
    }
}

private fun cheatModeButton(cheatMode: MutableProvider<Boolean>) = item {
    itemProvider by cheatMode.map { cheatMode ->
        if (cheatMode)
            DefaultGuiItems.TP_CHEATING_ON.clientsideProvider
        else DefaultGuiItems.TP_CHEATING_OFF.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && player.hasPermission(GIVE_PERMISSION)) {
            cheatMode.set(!cheatMode.get())
            player.playClickSound()
        }
    }
}

private fun itemCategoryTabButton(tab: Int, category: ItemCategory, activeTab: MutableProvider<Int>) = item {
    itemProvider by category.icon
    onClick {
        if (clickType.isLeftClick && activeTab.get() != tab) {
            player.playClickSound()
            activeTab.set(tab)
        }
    }
}

private fun playerInventoryTabButton(tab: Provider<Int>, activeTab: MutableProvider<Int>) = item {
    itemProvider by ItemBuilder(Material.CHEST).setName(Component.translatable("menu.nova.items.player_inventory"))
    onClick {
        if (clickType.isLeftClick && activeTab.get() != tab.get()) {
            player.playClickSound()
            activeTab.set(tab.get())
        }
    }
}

private fun tabPageBackItem(page: MutableProvider<Int>, pageCount: Provider<Int>) = item {
    itemProvider by combinedProvider(page, pageCount) { page, pageCount ->
        if (pageCount <= 1)
            ItemProvider.EMPTY
        else if (page > 0)
            DefaultGuiItems.TP_SMALL_ARROW_LEFT_ON.clientsideProvider
        else DefaultGuiItems.TP_SMALL_ARROW_LEFT_OFF.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() > 0) {
            page.set(page.get() - 1)
            player.playClickSound()
        }
    }
}

private fun tabPageForwardItem(page: MutableProvider<Int>, pageCount: Provider<Int>) = item {
    itemProvider by combinedProvider(page, pageCount) { page, pageCount ->
        if (pageCount <= 1)
            ItemProvider.EMPTY
        else if (page + 1 < pageCount)
            DefaultGuiItems.TP_SMALL_ARROW_RIGHT_ON.clientsideProvider
        else DefaultGuiItems.TP_SMALL_ARROW_RIGHT_OFF.clientsideProvider
    }
    onClick {
        if (clickType.isLeftClick && page.get() < pageCount.get() - 1) {
            page.set(page.get() + 1)
            player.playClickSound()
        }
    }
}

private fun Inventory.allowOnlyEquippable(slot: EquipmentSlot) {
    addPreUpdateHandler { event ->
        if (event.isRemove)
            return@addPreUpdateHandler
        if (event.newItem?.getData(DataComponentTypes.EQUIPPABLE)?.slot() != slot)
            event.isCancelled = true
    }
    addPostUpdateHandler { event ->
        val player = (event.updateReason as? PlayerUpdateReason)?.player()
            ?: return@addPostUpdateHandler
        val sound = event.newItem?.getData(DataComponentTypes.EQUIPPABLE)?.equipSound()
            ?: return@addPostUpdateHandler
        player.world.playSound(player.location, sound.toString(), SoundCategory.PLAYERS, 1f, 1f)
    }
}