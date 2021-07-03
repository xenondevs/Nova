package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.controlitem.PageItem
import de.studiocode.invui.resourcepack.Icon
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.item.novaItemBuilder

class PageBackItem : PageItem(false) {
    
    override fun getItemBuilder(gui: PagedGUI): ItemBuilder {
        val itemBuilder = (if (gui.hasPageBefore()) Icon.ARROW_1_LEFT else Icon.LIGHT_ARROW_1_LEFT).novaItemBuilder
        itemBuilder.setLocalizedName("menu.nova.paged.back")
        itemBuilder.addLocalizedLoreLines(
            if (gui.hasInfinitePages()) {
                if (gui.currentPageIndex == 0) TranslatableComponent("menu.nova.paged.limit_min")
                else TranslatableComponent("menu.nova.paged.go_inf", gui.currentPageIndex)
            } else {
                if (gui.hasPageBefore()) TranslatableComponent("menu.nova.paged.go", gui.currentPageIndex, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else TranslatableComponent("menu.nova.paged.limit_min")
            }
        )
        return itemBuilder
    }
    
}

class PageForwardItem : PageItem(true) {
    
    override fun getItemBuilder(gui: PagedGUI): ItemBuilder {
        val itemBuilder = (if (gui.hasNextPage()) Icon.ARROW_1_RIGHT else Icon.LIGHT_ARROW_1_RIGHT).novaItemBuilder
        itemBuilder.setLocalizedName("menu.nova.paged.forward")
        itemBuilder.addLocalizedLoreLines(
            if (gui.hasInfinitePages()) {
                TranslatableComponent("menu.nova.paged.go_inf", gui.currentPageIndex + 2)
            } else {
                if (gui.hasNextPage()) TranslatableComponent("menu.nova.paged.go", gui.currentPageIndex + 2, gui.pageAmount)
                    .apply { color = ChatColor.DARK_GRAY }
                else TranslatableComponent("menu.nova.paged.limit_max")
            }
        )
        return itemBuilder
    }
    
}