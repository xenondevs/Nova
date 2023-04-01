package xyz.xenondevs.nova.ui.item

import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.controlitem.ScrollItem
import xyz.xenondevs.nova.item.DefaultGuiMaterial

class ScrollUpItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) DefaultGuiMaterial.ARROW_1_UP.clientsideProvider else DefaultGuiMaterial.LIGHT_ARROW_1_UP.clientsideProvider
    
}

class ScrollDownItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) DefaultGuiMaterial.ARROW_1_DOWN.clientsideProvider else DefaultGuiMaterial.LIGHT_ARROW_1_DOWN.clientsideProvider
    
}

class ScrollLeftItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(-1)) DefaultGuiMaterial.ARROW_1_LEFT.clientsideProvider else DefaultGuiMaterial.LIGHT_ARROW_1_LEFT.clientsideProvider
    
}

class ScrollRightItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGui<*>): ItemProvider =
        if (gui.canScroll(1)) DefaultGuiMaterial.ARROW_1_RIGHT.clientsideProvider else DefaultGuiMaterial.LIGHT_ARROW_1_RIGHT.clientsideProvider
    
}