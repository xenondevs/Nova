package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.structure.Markers
import de.studiocode.invui.gui.structure.Structure.addGlobalIngredient
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.ui.item.PageBackItem
import xyz.xenondevs.nova.ui.item.PageForwardItem
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.ITEM_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', CoreGUIMaterial.INVENTORY_PART.itemProvider)
    addGlobalIngredient('-', CoreGUIMaterial.LIGHT_HORIZONTAL_LINE.itemProvider)
    addGlobalIngredient('|', CoreGUIMaterial.LIGHT_VERTICAL_LINE.itemProvider)
    addGlobalIngredient('1', CoreGUIMaterial.LIGHT_CORNER_TOP_LEFT.itemProvider)
    addGlobalIngredient('2', CoreGUIMaterial.LIGHT_CORNER_TOP_RIGHT.itemProvider)
    addGlobalIngredient('3', CoreGUIMaterial.LIGHT_CORNER_BOTTOM_LEFT.itemProvider)
    addGlobalIngredient('4', CoreGUIMaterial.LIGHT_CORNER_BOTTOM_RIGHT.itemProvider)
    addGlobalIngredient('5', CoreGUIMaterial.LIGHT_VERTICAL_RIGHT.itemProvider)
    addGlobalIngredient('6', CoreGUIMaterial.LIGHT_VERTICAL_LEFT.itemProvider)
    addGlobalIngredient('7', CoreGUIMaterial.LIGHT_HORIZONTAL_UP.itemProvider)
    addGlobalIngredient('8', CoreGUIMaterial.LIGHT_HORIZONTAL_DOWN.itemProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}