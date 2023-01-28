package xyz.xenondevs.nova.ui

import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.gui.structure.Structure.addGlobalIngredient
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.ui.item.PageBackItem
import xyz.xenondevs.nova.ui.item.PageForwardItem
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', CoreGUIMaterial.INVENTORY_PART.clientsideProvider)
    addGlobalIngredient('-', CoreGUIMaterial.LIGHT_HORIZONTAL_LINE.clientsideProvider)
    addGlobalIngredient('|', CoreGUIMaterial.LIGHT_VERTICAL_LINE.clientsideProvider)
    addGlobalIngredient('1', CoreGUIMaterial.LIGHT_CORNER_TOP_LEFT.clientsideProvider)
    addGlobalIngredient('2', CoreGUIMaterial.LIGHT_CORNER_TOP_RIGHT.clientsideProvider)
    addGlobalIngredient('3', CoreGUIMaterial.LIGHT_CORNER_BOTTOM_LEFT.clientsideProvider)
    addGlobalIngredient('4', CoreGUIMaterial.LIGHT_CORNER_BOTTOM_RIGHT.clientsideProvider)
    addGlobalIngredient('5', CoreGUIMaterial.LIGHT_VERTICAL_RIGHT.clientsideProvider)
    addGlobalIngredient('6', CoreGUIMaterial.LIGHT_VERTICAL_LEFT.clientsideProvider)
    addGlobalIngredient('7', CoreGUIMaterial.LIGHT_HORIZONTAL_UP.clientsideProvider)
    addGlobalIngredient('8', CoreGUIMaterial.LIGHT_HORIZONTAL_DOWN.clientsideProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}