package xyz.xenondevs.nova.ui.menu.explorer

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.NULL_PROVIDER
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mapEachIndexed
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.ScrollGuiDsl
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.gui
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.scrollItemsGui
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.ui.menu.item.installItemScrollSupport
import xyz.xenondevs.nova.ui.menu.item.scrollBar
import xyz.xenondevs.nova.ui.menu.item.scrollableItemProvider
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.createItemStack
import xyz.xenondevs.nova.world.item.itemProvider

private val windows = PlayerMapManager.createMap<Window>()
internal fun itemTagExplorer(player: Player) = windows.computeIfAbsent(player, ::createItemTagExplorer)
private fun createItemTagExplorer(player: Player) = window(player) {
    val tab = mutableProvider(0)
    val tags = ItemTypeTags.ALL_TAGS.map { tags -> tags.sortedBy { tag -> tag.tagKey.key().asString() } }
    
    title by combinedProvider(DefaultGuiTextures.TAGS, tags, tab) { texture, tags, tab -> 
        texture.getTitle(Component.text("#" + tags[tab].tagKey.key().asString())) 
    }.flatten()
    upperGui by gui(
        "k k k k . v v v v",
        "k k k k . v v v v",
        "k k k k . v v v v",
        "k k k k . v v v v",
        "k k k k . v v v v",
        "k k k k . v v v v",
    ) {
        'x' by Markers.CONTENT_LIST_SLOT_VERTICAL
        
        'k' by scrollItemsGui(
            "x x x x",
            "x x x x",
            "x x x x",
            "x x x x",
            "x x x x",
            "- - - -"
        ) {
            '-' by scrollBar(offset = 2)
            content by tags.mapEachIndexed { i, tag -> showTagButton(tag, i, tab) }
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
                    content by tag.entries.mapEach { itemTypeItem(it) }
                    background by DefaultGuiItems.DISABLED_SLOT.itemProvider
                }
            }
            this.tab by tab
        }
    }
    
}

context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
private fun showTagButton(tag: RegistryEntrySet.Paper.Tag<ItemType>, tab: Int, activeTab: MutableProvider<Int>) = item {
    itemProvider by itemProvider {
        base by tag.entries
            .flatMap { it.firstOrNull()?.itemProvider ?: NULL_PROVIDER }
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
        if (activeTab.get() != tab) {
            player.playClickSound()
            activeTab.set(tab)
        }
    }
    installItemScrollSupport()
}

context(windowDsl: WindowDsl, guiDsl: ScrollGuiDsl<*>)
private fun itemTypeItem(type: RegistryEntry.Paper<ItemType>) = item {
    itemProvider by type.itemProvider.map { scrollableItemProvider(it) }
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