package xyz.xenondevs.nova.ui

import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.gui.structure.Structure.addGlobalIngredient
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.ui.item.PageBackItem
import xyz.xenondevs.nova.ui.item.PageForwardItem
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', DefaultGuiItems.INVENTORY_PART.clientsideProvider)
    addGlobalIngredient('-', DefaultGuiItems.LIGHT_HORIZONTAL_LINE.clientsideProvider)
    addGlobalIngredient('|', DefaultGuiItems.LIGHT_VERTICAL_LINE.clientsideProvider)
    addGlobalIngredient('1', DefaultGuiItems.LIGHT_CORNER_TOP_LEFT.clientsideProvider)
    addGlobalIngredient('2', DefaultGuiItems.LIGHT_CORNER_TOP_RIGHT.clientsideProvider)
    addGlobalIngredient('3', DefaultGuiItems.LIGHT_CORNER_BOTTOM_LEFT.clientsideProvider)
    addGlobalIngredient('4', DefaultGuiItems.LIGHT_CORNER_BOTTOM_RIGHT.clientsideProvider)
    addGlobalIngredient('5', DefaultGuiItems.LIGHT_VERTICAL_RIGHT.clientsideProvider)
    addGlobalIngredient('6', DefaultGuiItems.LIGHT_VERTICAL_LEFT.clientsideProvider)
    addGlobalIngredient('7', DefaultGuiItems.LIGHT_HORIZONTAL_UP.clientsideProvider)
    addGlobalIngredient('8', DefaultGuiItems.LIGHT_HORIZONTAL_DOWN.clientsideProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}