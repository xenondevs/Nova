package xyz.xenondevs.nova.ui.item

import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem
import xyz.xenondevs.nova.item.DefaultGuiItems

class ScrollUpItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) DefaultGuiItems.ARROW_1_UP.clientsideProvider else DefaultGuiItems.LIGHT_ARROW_1_UP.clientsideProvider
    
}

class ScrollDownItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) DefaultGuiItems.ARROW_1_DOWN.clientsideProvider else DefaultGuiItems.LIGHT_ARROW_1_DOWN.clientsideProvider
    
}

class ScrollLeftItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) DefaultGuiItems.ARROW_1_LEFT.clientsideProvider else DefaultGuiItems.LIGHT_ARROW_1_LEFT.clientsideProvider
    
}

class ScrollRightItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) DefaultGuiItems.ARROW_1_RIGHT.clientsideProvider else DefaultGuiItems.LIGHT_ARROW_1_RIGHT.clientsideProvider
    
}