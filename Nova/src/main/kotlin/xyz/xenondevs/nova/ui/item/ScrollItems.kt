package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.gui.impl.ScrollGUI
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.controlitem.ScrollItem
import xyz.xenondevs.nova.material.CoreGUIMaterial

class ScrollUpItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGUI): ItemProvider =
        if (gui.canScroll(-1)) CoreGUIMaterial.ARROW_1_UP.itemProvider else CoreGUIMaterial.LIGHT_ARROW_1_UP.itemProvider
    
}

class ScrollDownItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGUI): ItemProvider =
        if (gui.canScroll(1)) CoreGUIMaterial.ARROW_1_DOWN.itemProvider else CoreGUIMaterial.LIGHT_ARROW_1_DOWN.itemProvider
    
}

class ScrollLeftItem : ScrollItem(-1) {
    
    override fun getItemProvider(gui: ScrollGUI): ItemProvider =
        if (gui.canScroll(-1)) CoreGUIMaterial.ARROW_1_LEFT.itemProvider else CoreGUIMaterial.LIGHT_ARROW_1_LEFT.itemProvider
    
}

class ScrollRightItem : ScrollItem(1) {
    
    override fun getItemProvider(gui: ScrollGUI): ItemProvider =
        if (gui.canScroll(1)) CoreGUIMaterial.ARROW_1_RIGHT.itemProvider else CoreGUIMaterial.LIGHT_ARROW_1_RIGHT.itemProvider
    
}