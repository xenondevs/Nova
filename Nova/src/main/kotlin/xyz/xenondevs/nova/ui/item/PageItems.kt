package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.PagedGUI
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.controlitem.PageItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.setLocalizedName

class PageBackItem : PageItem(false) {
    
    override fun getItemProvider(gui: PagedGUI): ItemProvider {
        val itemBuilder = (if (gui.hasPageBefore()) CoreGUIMaterial.ARROW_1_LEFT else CoreGUIMaterial.LIGHT_ARROW_1_LEFT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName("menu.nova.paged.back")
        itemBuilder.addLoreLines(
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
    
    override fun getItemProvider(gui: PagedGUI): ItemProvider {
        val itemBuilder = (if (gui.hasNextPage()) CoreGUIMaterial.ARROW_1_RIGHT else CoreGUIMaterial.LIGHT_ARROW_1_RIGHT).createClientsideItemBuilder()
        itemBuilder.setLocalizedName("menu.nova.paged.forward")
        itemBuilder.addLoreLines(
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