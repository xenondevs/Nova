package xyz.xenondevs.nova.ui.menu.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.item.DefaultGuiItems

/**
 * A UI item for [PagedGuis][PagedGui] that goes back one page on left-click.
 */
class PageBackItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_LEFT_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_LEFT_OFF.clientsideProvider
) : AbstractPagedGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider {
        val itemBuilder = ItemBuilder((if (gui.page > 0) on else off).get())
        itemBuilder.setName(Component.translatable("menu.nova.paged.back", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.page > 0)
                Component.translatable(
                    "menu.nova.paged.go", NamedTextColor.DARK_GRAY,
                    Component.text(gui.page), Component.text(gui.pageCount)
                )
            else Component.translatable("menu.nova.paged.limit_min", NamedTextColor.DARK_GRAY)
        )
        return itemBuilder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.page > 0) {
            player.playClickSound()
            gui.page--
        }
    }
    
}

/**
 * A UI item for [PagedGuis][PagedGui] that goes forward one page on left-click.
 */
class PageForwardItem(
    private val on: ItemProvider = DefaultGuiItems.ARROW_RIGHT_ON.clientsideProvider,
    private val off: ItemProvider = DefaultGuiItems.ARROW_RIGHT_OFF.clientsideProvider
) : AbstractPagedGuiBoundItem() {
    
    override fun getItemProvider(player: Player): ItemProvider {
        val itemBuilder = ItemBuilder((if (gui.page + 1 < gui.pageCount) on else off).get())
        itemBuilder.setName(Component.translatable("menu.nova.paged.forward", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.page + 1 < gui.pageCount)
                Component.translatable(
                    "menu.nova.paged.go", NamedTextColor.DARK_GRAY,
                    Component.text(gui.page + 2), Component.text(gui.pageCount)
                )
            else Component.translatable("menu.nova.paged.limit_max", NamedTextColor.DARK_GRAY)
        )
        return itemBuilder
    }
    
    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        if (clickType == ClickType.LEFT && gui.page + 1 < gui.pageCount) {
            player.playClickSound()
            gui.page++
        }
    }
    
}