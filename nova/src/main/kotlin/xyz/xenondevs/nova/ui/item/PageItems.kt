package xyz.xenondevs.nova.ui.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.addLoreLines
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.controlitem.PageItem
import xyz.xenondevs.nova.material.CoreGuiMaterial

class PageBackItem : PageItem(false) {
    
    override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
        val itemBuilder = (if (gui.hasPreviousPage()) CoreGuiMaterial.ARROW_1_LEFT else CoreGuiMaterial.LIGHT_ARROW_1_LEFT).createClientsideItemBuilder()
        itemBuilder.setDisplayName(Component.translatable("menu.nova.paged.back", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                if (gui.currentPage == 0) Component.translatable("menu.nova.paged.limit_min", NamedTextColor.DARK_GRAY)
                else Component.translatable("menu.nova.paged.go_infinite", NamedTextColor.DARK_GRAY, Component.text(gui.currentPage))
            } else {
                if (gui.hasPreviousPage())
                    Component.translatable(
                        "menu.nova.paged.go",
                        NamedTextColor.DARK_GRAY,
                        Component.text(gui.currentPage),
                        Component.text(gui.pageAmount)
                    )
                else Component.translatable("menu.nova.paged.limit_min", NamedTextColor.DARK_GRAY)
            }
        )
        return itemBuilder
    }
    
}

class PageForwardItem : PageItem(true) {
    
    override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
        val itemBuilder = (if (gui.hasNextPage()) CoreGuiMaterial.ARROW_1_RIGHT else CoreGuiMaterial.LIGHT_ARROW_1_RIGHT).createClientsideItemBuilder()
        itemBuilder.setDisplayName(Component.translatable("menu.nova.paged.forward", NamedTextColor.GRAY))
        itemBuilder.addLoreLines(
            if (gui.hasInfinitePages()) {
                Component.translatable(
                    "menu.nova.paged.go_infinite",
                    NamedTextColor.DARK_GRAY,
                    Component.text(gui.currentPage + 2)
                )
            } else {
                if (gui.hasNextPage())
                    Component.translatable(
                        "menu.nova.paged.go",
                        NamedTextColor.DARK_GRAY,
                        Component.text(gui.currentPage + 2),
                        Component.text(gui.pageAmount)
                    )
                else Component.translatable("menu.nova.paged.limit_max", NamedTextColor.DARK_GRAY)
            }
        )
        return itemBuilder
    }
    
}