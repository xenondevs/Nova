package xyz.xenondevs.nova.ui

import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.gui.structure.Structure.addGlobalIngredient
import xyz.xenondevs.nova.material.DefaultGuiMaterial
import xyz.xenondevs.nova.ui.item.PageBackItem
import xyz.xenondevs.nova.ui.item.PageForwardItem
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', DefaultGuiMaterial.INVENTORY_PART.clientsideProvider)
    addGlobalIngredient('-', DefaultGuiMaterial.LIGHT_HORIZONTAL_LINE.clientsideProvider)
    addGlobalIngredient('|', DefaultGuiMaterial.LIGHT_VERTICAL_LINE.clientsideProvider)
    addGlobalIngredient('1', DefaultGuiMaterial.LIGHT_CORNER_TOP_LEFT.clientsideProvider)
    addGlobalIngredient('2', DefaultGuiMaterial.LIGHT_CORNER_TOP_RIGHT.clientsideProvider)
    addGlobalIngredient('3', DefaultGuiMaterial.LIGHT_CORNER_BOTTOM_LEFT.clientsideProvider)
    addGlobalIngredient('4', DefaultGuiMaterial.LIGHT_CORNER_BOTTOM_RIGHT.clientsideProvider)
    addGlobalIngredient('5', DefaultGuiMaterial.LIGHT_VERTICAL_RIGHT.clientsideProvider)
    addGlobalIngredient('6', DefaultGuiMaterial.LIGHT_VERTICAL_LEFT.clientsideProvider)
    addGlobalIngredient('7', DefaultGuiMaterial.LIGHT_HORIZONTAL_UP.clientsideProvider)
    addGlobalIngredient('8', DefaultGuiMaterial.LIGHT_HORIZONTAL_DOWN.clientsideProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}