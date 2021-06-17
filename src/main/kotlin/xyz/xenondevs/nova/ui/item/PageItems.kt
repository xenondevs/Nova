package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.controlitem.PageItem
import de.studiocode.invui.resourcepack.Icon

class PageBackItem : PageItem(false) {
    
    override fun getItemBuilder(gui: PagedGUI): ItemBuilder {
        val itemBuilder = (if (gui.hasPageBefore()) Icon.ARROW_1_LEFT else Icon.LIGHT_ARROW_1_LEFT).itemBuilder
        itemBuilder.displayName = "§7Go back"
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                if (gui.currentPageIndex == 0) "§cYou can't go further back"
                else "§8Go to page ${gui.currentPageIndex}"
            } else {
                if (gui.hasPageBefore()) "§8Go to page ${gui.currentPageIndex}/${gui.pageAmount}"
                else "§8You can't go further back"
            }
        )
        return itemBuilder
    }
    
}

class PageForwardItem : PageItem(true) {
    
    override fun getItemBuilder(gui: PagedGUI): ItemBuilder {
        val itemBuilder = (if (gui.hasNextPage()) Icon.ARROW_1_RIGHT else Icon.LIGHT_ARROW_1_RIGHT).itemBuilder
        itemBuilder.displayName = "§7Next page"
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                "§8Go to page ${gui.currentPageIndex + 2}"
            } else {
                if (gui.hasNextPage()) "§8Go to page ${gui.currentPageIndex + 2}/${gui.pageAmount}"
                else "§8There are no more pages"
            }
        )
        return itemBuilder
    }
    
}