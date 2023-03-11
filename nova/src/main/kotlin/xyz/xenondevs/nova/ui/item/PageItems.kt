package xyz.xenondevs.nova.ui.item

import net.md_5.bungee.api.ChatColor
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.PageItem
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.util.component.bungee.addLoreLines
import xyz.xenondevs.nova.util.component.bungee.localized
import xyz.xenondevs.nova.util.component.bungee.setLocalizedName

class PageBackItem : PageItem(false) {
    
    override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
        val itemBuilder = (if (gui.hasPreviousPage()) CoreGuiMaterial.ARROW_1_LEFT else CoreGuiMaterial.LIGHT_ARROW_1_LEFT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.paged.back")
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                if (gui.currentPage == 0) localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_min")
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.go_infinite", gui.currentPage)
            } else {
                if (gui.hasPreviousPage()) localized(ChatColor.DARK_GRAY, "menu.nova.paged.go", gui.currentPage, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_min")
            }
        )
        return itemBuilder
    }
    
}

class PageForwardItem : PageItem(true) {
    
    override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
        val itemBuilder = (if (gui.hasNextPage()) CoreGuiMaterial.ARROW_1_RIGHT else CoreGuiMaterial.LIGHT_ARROW_1_RIGHT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.paged.forward")
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                localized(ChatColor.DARK_GRAY, "menu.nova.paged.go_infinite", gui.currentPage + 2)
            } else {
                if (gui.hasNextPage()) localized(ChatColor.DARK_GRAY, "menu.nova.paged.go", gui.currentPage + 2, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_max")
            }
        )
        return itemBuilder
    }
    
}