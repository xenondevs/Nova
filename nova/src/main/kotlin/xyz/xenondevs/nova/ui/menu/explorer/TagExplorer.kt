package xyz.xenondevs.nova.ui.menu.explorer

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Keyed
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mapEachIndexed
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ScrollGuiDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.anvilWindow
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.scrollItemsGui
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.registry.tags.ItemTypeTags
import xyz.xenondevs.nova.ui.menu.item.installItemScrollSupport
import xyz.xenondevs.nova.ui.menu.item.scrollBar
import xyz.xenondevs.nova.ui.menu.item.scrollableItemProvider
import xyz.xenondevs.nova.ui.menu.locale
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.ui.overlay.guitexture.component
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.block.itemTypeOrNull
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.createItemStack
import xyz.xenondevs.nova.world.item.itemProvider

private val itemTagWindows = PlayerMapManager.createMap<Window>()

internal fun itemTagExplorer(player: Player): Window =
    itemTagWindows.computeIfAbsent(player, ::createItemTagExplorer)

private fun createItemTagExplorer(player: Player): Window =
    createTagExplorer(player, ItemTypeTags.ALL_TAGS) { it }

private val blockTagWindows = PlayerMapManager.createMap<Window>()

internal fun blockTagExplorer(player: Player): Window =
    blockTagWindows.computeIfAbsent(player, ::createBlockTagExplorer)

private fun createBlockTagExplorer(player: Player): Window =
    createTagExplorer(player, BlockTypeTags.ALL_TAGS) { it.itemTypeOrNull ?: ItemType.AIR }

internal fun <T : Keyed> createTagExplorer(
    player: Player,
    allTags: Provider<Set<RegistryEntrySet.Paper.Tag<T>>>,
    elementToItemType: (T) -> ItemType
): Window {
    val tab = mutableProvider(0)
    val tags = allTags.map { tags -> tags.sortedBy { tag -> tag.tagKey.key().asString() } }
    lateinit var searchWindow: Provider<Window>
    val mainWindow = provider {
        window(player) {
            title by combinedProvider(DefaultGuiTextures.TAGS, tags, tab) { texture, tags, tab ->
                texture.getTitle([Component.text("#" + tags[tab].tagKey.key().asString())], locale)
            }.flatten()
            upperGui by gui(
                "k k k k s v v v v",
                "k k k k . v v v v",
                "k k k k . v v v v",
                "k k k k . v v v v",
                "k k k k . v v v v",
                "k k k k . v v v v",
            ) {
                'x' by Markers.CONTENT_LIST_SLOT_VERTICAL
                's' by searchButton(searchWindow)
                
                'k' by scrollItemsGui(
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "- - - -"
                ) {
                    '-' by scrollBar(offset = 2)
                    content by tags.mapEachIndexed { i, tag ->
                        showTagButton(tag, i, tab, elementToItemType)
                    }
                }
                
                'v' by tabGui(
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "x x x x",
                    "x x x x"
                ) {
                    tabs by tags.mapEach { tag ->
                        scrollItemsGui(
                            "x x x x",
                            "x x x x",
                            "x x x x",
                            "x x x x",
                            "x x x x",
                            "- - - -",
                        ) {
                            '-' by scrollBar(offset = 2)
                            content by tag.entries.mapEach { entry ->
                                itemTypeItem(entry.map(elementToItemType))
                            }
                            background by DefaultGuiItems.DISABLED_SLOT.itemProvider
                        }
                    }
                    this.tab by tab
                }
            }
        }
    }
    
    searchWindow = provider {
        anvilWindow(player) {
            title by DefaultGuiTextures.SEARCH.component
            lowerGui by scrollItemsGui(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                ". - - - - - - - ^"
            ) {
                'x' by Markers.CONTENT_LIST_SLOT_VERTICAL
                '-' by scrollBar(offset = -2)
                '^' by closeSearchButton(mainWindow)
                content by combinedProvider(text, tags) { filter, tags ->
                    val query = filter.removePrefix("#")
                    tags.withIndex().filter { indexedTag ->
                        indexedTag.value.tagKey.key().asString().contains(query, ignoreCase = true)
                    }
                }.mapEach { indexedTag ->
                    showTagButton(indexedTag.value, indexedTag.index, tab, elementToItemType, mainWindow)
                }
            }
            
            fallbackWindow by mainWindow
            onOutsideClick { isCancelled = true }
        }
    }
    
    return mainWindow.get()
}

context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
private fun <T : Keyed> showTagButton(
    tag: RegistryEntrySet.Paper.Tag<T>,
    tab: Int,
    activeTab: MutableProvider<Int>,
    elementToItemType: (T) -> ItemType,
    mainWindow: Provider<Window>? = null
) = item {
    itemProvider by itemProvider {
        base by tag.entries
            .flatMap { entries ->
                entries.firstOrNull()
                    ?.map(elementToItemType)
                    ?.flatMap { it.itemProvider }
                    ?: NULL_PROVIDER
            }
            .map { it?.get() ?: ItemStack.empty() }
            .map { scrollableItemProvider(it).get() }
        name by Component.text("#" + tag.tagKey.key().asString(), NamedTextColor.GRAY)
        lore by emptyList()
        data[DataComponentTypes.TOOLTIP_DISPLAY] by TooltipDisplay
            .tooltipDisplay()
            .hiddenComponents(Registry.DATA_COMPONENT_TYPE.toSet())
            .build()
        hasGlint by activeTab.map { it == tab }
    }
    onClick {
        if (activeTab.get() != tab || mainWindow != null) {
            player.playClickSound()
            activeTab.set(tab)
        }
        mainWindow?.get()?.open()
    }
    installItemScrollSupport()
}

context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
private fun itemTypeItem(type: Provider<ItemType>) = item {
    itemProvider by type.flatMap { it.itemProvider }.map { scrollableItemProvider(it) }
    onClick {
        val itemStack = type.createItemStack()
        when (clickType) {
            ClickType.LEFT, ClickType.RIGHT -> {
                val cursor = player.itemOnCursor
                if (cursor.isEmpty || cursor.isSimilar(itemStack)) {
                    player.setItemOnCursor(itemStack.apply { amount = cursor.amount + 1 })
                }
            }
            
            ClickType.MIDDLE -> {
                player.setItemOnCursor(itemStack.apply { amount = itemStack.maxStackSize })
            }
            
            ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT -> {
                player.inventory.addItem(itemStack.apply { amount = itemStack.maxStackSize })
            }
            
            else -> Unit
        }
    }
    installItemScrollSupport()
}

private fun searchButton(searchWindow: Provider<Window>) = item {
    itemProvider by DefaultGuiItems.TP_SEARCH.itemProvider
    onClick {
        if (clickType.isLeftClick) {
            searchWindow.get().open()
            player.playClickSound()
        }
    }
}

private fun closeSearchButton(mainWindow: Provider<Window>) = item {
    itemProvider by DefaultGuiItems.TP_ARROW_UP_ON.itemProvider
    onClick {
        if (clickType.isLeftClick) {
            mainWindow.get().open()
            player.playClickSound()
        }
    }
}
