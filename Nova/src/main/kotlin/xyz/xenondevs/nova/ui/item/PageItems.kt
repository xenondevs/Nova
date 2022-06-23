package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.controlitem.PageItem
import net.md_5.bungee.api.ChatColor
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.data.setLocalizedName

class PageBackItem : PageItem(false) {
    
    override fun getItemProvider(gui: PagedGUI): ItemProvider {
        val itemBuilder = (if (gui.hasPageBefore()) CoreGUIMaterial.ARROW_1_LEFT else CoreGUIMaterial.LIGHT_ARROW_1_LEFT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.paged.back")
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                if (gui.currentPageIndex == 0) localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_min")
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.go_infinite", gui.currentPageIndex)
            } else {
                if (gui.hasPageBefore()) localized(ChatColor.DARK_GRAY, "menu.nova.paged.go", gui.currentPageIndex, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_min")
            }
        )
        return itemBuilder
    }
    
}

class PageForwardItem : PageItem(true) {
    
    override fun getItemProvider(gui: PagedGUI): ItemProvider {
        val itemBuilder = (if (gui.hasNextPage()) CoreGUIMaterial.ARROW_1_RIGHT else CoreGUIMaterial.LIGHT_ARROW_1_RIGHT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName(ChatColor.GRAY, "menu.nova.paged.forward")
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                localized(ChatColor.DARK_GRAY, "menu.nova.paged.go_infinite", gui.currentPageIndex + 2)
            } else {
                if (gui.hasNextPage()) localized(ChatColor.DARK_GRAY, "menu.nova.paged.go", gui.currentPageIndex + 2, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else localized(ChatColor.DARK_GRAY, "menu.nova.paged.limit_max")
            }
        )
        return itemBuilder
    }
    
}