package xyz.xenondevs.nova.ui.menu.explorer.creative

import me.xdrop.fuzzywuzzy.FuzzySearch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.gui.setContent
import xyz.xenondevs.invui.gui.setTab
import xyz.xenondevs.invui.gui.setTabs
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem
import xyz.xenondevs.invui.item.AbstractTabGuiBoundItem
import xyz.xenondevs.invui.item.Click
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.setItemProvider
import xyz.xenondevs.invui.window.AnvilWindow
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.addRenameHandler
import xyz.xenondevs.invui.window.setTitle
import xyz.xenondevs.nova.ui.menu.applyDefaultTPIngredients
import xyz.xenondevs.nova.ui.menu.explorer.ItemMenu
import xyz.xenondevs.nova.ui.menu.item.AnvilTextItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.ItemCategories
import xyz.xenondevs.nova.world.item.ItemCategory

private val TAB_BUTTON_TEXTURES = arrayOf(
    DefaultGuiTextures.ITEMS_0,
    DefaultGuiTextures.ITEMS_1,
    DefaultGuiTextures.ITEMS_2,
    DefaultGuiTextures.ITEMS_3,
    DefaultGuiTextures.ITEMS_4
)

private const val GIVE_PERMISSION = "nova.command.give"

internal class ItemsMenu(val player: Player) : ItemMenu {
    
    private val cheatMode: MutableProvider<Boolean> = mutableProvider { player.persistentDataContainer.get(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN) == true }
        .apply {
            subscribe { cheatMode ->
                player.persistentDataContainer.set(CHEAT_MODE_KEY, PersistentDataType.BOOLEAN, cheatMode)
            }
        }
    private val activeTab: MutableProvider<Int> = mutableProvider(0)
    private val filter: MutableProvider<String> = mutableProvider("")
    
    private val filteredItems: Provider<List<Item>> = combinedProvider(filter, ItemCategories.obtainableItems) { filter, obtainableItems ->
        if (filter.isNotEmpty()) {
            val names = obtainableItems
                .asSequence()
                .map { it to it.name.toPlainText(player) }
                .filter { (_, name) -> name.contains(filter, true) }
                .toMap(HashMap())
            val scores = FuzzySearch.extractAll(filter, names.values).associateTo(HashMap()) { it.string to it.score }
            names.keys.sortedWith { o1, o2 ->
                val s1 = scores[names[o1]]!!
                val s2 = scores[names[o2]]!!
                if (s1 == s2) {
                    val i1 = obtainableItems.indexOf(o1)
                    val i2 = obtainableItems.indexOf(o2)
                    i1.compareTo(i2)
                } else s1.compareTo(s2)
            }
        } else obtainableItems.toList()
    }
    
    private val tabButtons: MutableProvider<Provider<List<CreativeTabItem>>> = mutableProvider(provider(emptyList()))
    
    private val openSearchItem: Item = Item.builder()
        .setItemProvider(DefaultGuiItems.TP_SEARCH.clientsideProvider)
        .addClickHandler { _, click ->
            if (click.clickType == ClickType.LEFT) {
                searchWindow.open()
                click.player.playClickSound()
            }
        }.build()
    
    private val cheatModeItem: Item = Item.builder()
        .setItemProvider(cheatMode) { cheatMode ->
            if (cheatMode)
                DefaultGuiItems.TP_CHEATING_ON.clientsideProvider
            else DefaultGuiItems.TP_CHEATING_OFF.clientsideProvider
        }.addClickHandler { _, click ->
            if (click.clickType == ClickType.LEFT && player.hasPermission(GIVE_PERMISSION)) {
                cheatMode.set(!cheatMode.get())
                click.player.playClickSound()
            }
        }.build()
    
    private val mainWindow = Window.single()
        .setTitle(activeTab) { tab -> TAB_BUTTON_TEXTURES[tab % 5].component }
        .setGui(TabGui.normal()
            .setStructure(
                "p p p p p p p p p",
                "p p p p p p p p p",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x"
            )
            .addIngredient('p', PagedGui.items()
                .setStructure(
                    "x . x . x . x . x",
                    "< . . . . . . . >"
                )
                .addIngredient('<', TabPageBackItem())
                .addIngredient('>', TabPageForwardItem())
                .addPageChangeHandler { _, page -> activeTab.set(page * 5) }
                .setContent(tabButtons.flatten())
                .build()
            )
            .setTab(activeTab)
            .setTabs(ItemCategories.categories.mapEach { category ->
                ScrollGui.items()
                    .setStructure(
                        "x x x x x x x x s",
                        "x x x x x x x x c",
                        "x x x x x x x x u",
                        "x x x x x x x x d"
                    )
                    .applyDefaultTPIngredients()
                    .addIngredient('s', openSearchItem)
                    .addIngredient('c', cheatModeItem)
                    .setContent(category.items)
                    .build()
            })
            .addModifier { tabGui ->
                // because the tab gui buttons are placed in a paged gui, they need to be bound manually
                val buttons = ItemCategories.categories.map { categories ->
                    categories
                        .mapIndexed { i, category -> CreativeTabItem(i, category) }
                        .onEach { it.bind(tabGui) }
                }
                tabButtons.set(buttons)
            }
        )
        .build(player)
    
    private val searchWindow = AnvilWindow.split()
        .setTitle(DefaultGuiTextures.SEARCH.getTitle("menu.nova.items.search"))
        .setUpperGui(Gui.normal()
            .setStructure("a . .")
            .addIngredient('a', AnvilTextItem(DefaultGuiItems.INVISIBLE_ITEM.createClientsideItemBuilder(), ""))
        )
        .setLowerGui(PagedGui.items()
            .setStructure(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "# # # < # > # # s"
            )
            .addIngredient('s', Item.builder()
                .setItemProvider(
                    DefaultGuiItems.ARROW_UP_ON.createClientsideItemBuilder()
                        .setName(Component.translatable("menu.nova.items.search.back", NamedTextColor.GRAY))
                )
                .addClickHandler { _, click ->
                    if (click.clickType == ClickType.LEFT) {
                        if (filter.get().isBlank()) mainWindow.open() else searchResultsWindow.open()
                        click.player.playClickSound()
                    }
                }
            )
            .setContent(filteredItems)
            .build()
        )
        .addRenameHandler(filter)
        .build(player)
    
    private val searchResultsWindow = Window.single()
        .setTitle(filter) { filter ->
            val title = Component.text()
                .append(Component.translatable("menu.nova.items"))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(filter, NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .build()
            DefaultGuiTextures.EMPTY_GUI.getTitle(title)
        }
        .setGui(PagedGui.items()
            .setStructure(
                ". . . < s > . . .",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x"
            )
            .applyDefaultTPIngredients()
            .addIngredient('s', openSearchItem)
            .setContent(filteredItems)
            .build()
        )
        .build(player)
    
    override fun show() {
        ItemMenu.addToHistory(player.uniqueId, this)
        mainWindow.open()
    }
    
    private inner class CreativeTabItem(private val tab: Int, private val category: ItemCategory) : AbstractTabGuiBoundItem() {
        
        override fun getItemProvider(player: Player) = category.icon
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            if (clickType == ClickType.LEFT && gui.isTabAvailable(tab) && gui.tab != tab) {
                player.playClickSound()
                gui.tab = tab
            }
        }
        
    }
    
    private class TabPageBackItem : AbstractPagedGuiBoundItem() {
        
        override fun getItemProvider(player: Player): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasPreviousPage())
                DefaultGuiItems.TP_SMALL_ARROW_LEFT_ON.clientsideProvider
            else DefaultGuiItems.TP_SMALL_ARROW_LEFT_OFF.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            if (clickType == ClickType.LEFT) {
                gui.page--
                player.playClickSound()
            }
        }
        
    }
    
    private class TabPageForwardItem : AbstractPagedGuiBoundItem() {
        
        override fun getItemProvider(player: Player): ItemProvider {
            return if (gui.pageAmount <= 1)
                ItemProvider.EMPTY
            else if (gui.hasNextPage())
                DefaultGuiItems.TP_SMALL_ARROW_RIGHT_ON.clientsideProvider
            else DefaultGuiItems.TP_SMALL_ARROW_RIGHT_OFF.clientsideProvider
        }
        
        override fun handleClick(clickType: ClickType, player: Player, click: Click) {
            if (clickType == ClickType.LEFT) {
                gui.page++
                player.playClickSound()
            }
        }
        
    }
    
    companion object {
        val CHEAT_MODE_KEY = NamespacedKey("nova", "cheat_mode")
    }
    
}