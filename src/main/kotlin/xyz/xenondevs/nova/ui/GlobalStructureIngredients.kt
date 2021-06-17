package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.structure.Marker
import de.studiocode.invui.gui.structure.Structure.addGlobalIngredient
import de.studiocode.invui.resourcepack.Icon
import xyz.xenondevs.nova.ui.item.PageBackItem
import xyz.xenondevs.nova.ui.item.PageForwardItem
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

fun setGlobalIngredients() {
    addGlobalIngredient('x', Marker.ITEM_LIST_SLOT)
    addGlobalIngredient('#', Icon.BACKGROUND.item)
    addGlobalIngredient('-', Icon.LIGHT_HORIZONTAL_LINE.item)
    addGlobalIngredient('|', Icon.LIGHT_VERTICAL_LINE.item)
    addGlobalIngredient('1', Icon.LIGHT_CORNER_TOP_LEFT.item)
    addGlobalIngredient('2', Icon.LIGHT_CORNER_TOP_RIGHT.item)
    addGlobalIngredient('3', Icon.LIGHT_CORNER_BOTTOM_LEFT.item)
    addGlobalIngredient('4', Icon.LIGHT_CORNER_BOTTOM_RIGHT.item)
    addGlobalIngredient('5', Icon.LIGHT_VERTICAL_RIGHT.item)
    addGlobalIngredient('6', Icon.LIGHT_VERTICAL_LEFT.item)
    addGlobalIngredient('7', Icon.LIGHT_HORIZONTAL_UP.item)
    addGlobalIngredient('8', Icon.LIGHT_HORIZONTAL_DOWN.item)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}