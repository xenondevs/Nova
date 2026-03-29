package xyz.xenondevs.nova.ui.menu.explorer

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import io.papermc.paper.registry.RegistryKey
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
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.commons.provider.mapEachIndexed
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.invui.dsl.by
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.scrollItemsGui
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.ui.menu.item.scrollLeftItem
import xyz.xenondevs.nova.ui.menu.item.scrollRightItem
import xyz.xenondevs.nova.ui.menu.item.scrollerItem
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider
import xyz.xenondevs.nova.world.item.createItemStack

private val windows = PlayerMapManager.createMap<Window>()
internal fun itemTagExplorer(player: Player) = windows.computeIfAbsent(player, ::createItemTagExplorer)
private fun createItemTagExplorer(player: Player) = window(player) {
    val tab = mutableProvider(0)
    val tags = combinedProvider(
        NovaRegistries.ITEM.tags,
        ItemTypeTags.ALL_TAGS
    ) { novaTags, paperTags ->
        buildSet {
            addAll(novaTags.map { it.tagKey })
            addAll(paperTags.map { it.tagKey.key() })
        }.sortedBy { it.asString() }.map { registryEntrySetOf(it, NovaRegistries.ITEM, RegistryKey.ITEM) }
    }
    
    title by combinedProvider(tags, tab) { tags, tab -> "#" + tags[tab].tagKey.asString() }
    upperGui by scrollItemsGui(
        "x x x # t t t t t",
        "x x x # t t t t t",
        "x x x # t t t t t",
        "x x x # t t t t t",
        "x x x # t t t t t",
        "< s > # t t t t t",
    ) {
        'x' by Markers.CONTENT_LIST_SLOT_VERTICAL
        '<' by scrollLeftItem(DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider, DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider)
        's' by scrollerItem(DefaultGuiItems.SCROLLER_HORIZONTAL.clientsideProvider)
        '>' by scrollRightItem(DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider, DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider)
        
        content by tags.mapEachIndexed { i, tag -> showTagButton(tag, i, tab) }
        
        't' by tabGui(
            "x x x x x",
            "x x x x x",
            "x x x x x",
            "x x x x x",
            "x x x x x",
            "x x x x x"
        ) {
            tabs by tags.mapEach { tag ->
                scrollItemsGui(
                    "x x x x x",
                    "x x x x x",
                    "x x x x x",
                    "x x x x x",
                    "x x x x x",
                    "# < s > #",
                ) {
                    '<' by scrollLeftItem(DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider, DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider)
                    's' by scrollerItem(DefaultGuiItems.SCROLLER_HORIZONTAL.clientsideProvider)
                    '>' by scrollRightItem(DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider, DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider)
                    content by tag.entries.mapEach(::itemTypeItem)
                    background by DefaultGuiItems.DISABLED_SLOT.clientsideProvider
                }
            }
            this.tab by tab
        }
    }
    
}

private fun showTagButton(tag: RegistryEntrySet.Mixed.Tag<NovaItem, ItemType>, tab: Int, activeTab: MutableProvider<Int>) = item {
    itemProvider by itemProvider {
        base by tag.entries
            .flatMap { it.firstOrNull()?.clientsideProvider ?: NULL_PROVIDER }
            .map { it?.get() ?: ItemStack.empty() }
        name by Component.text("#" + tag.tagKey.asString(), NamedTextColor.GRAY)
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
}

private fun itemTypeItem(type: RegistryEntry.Either<NovaItem, ItemType>) = item {
    itemProvider by type.clientsideProvider
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
}