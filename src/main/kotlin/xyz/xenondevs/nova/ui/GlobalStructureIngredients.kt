package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.structure.Marker
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.resourcepack.Icon
import xyz.xenondevs.nova.ui.item.ScrollDownItem
import xyz.xenondevs.nova.ui.item.ScrollUpItem

fun setGlobalIngredients() {
    Structure.addGlobalIngredient('x', Marker.ITEM_LIST_SLOT)
    Structure.addGlobalIngredient('#', Icon.BACKGROUND.item)
    Structure.addGlobalIngredient('-', Icon.LIGHT_HORIZONTAL_LINE.item)
    Structure.addGlobalIngredient('|', Icon.LIGHT_VERTICAL_LINE.item)
    Structure.addGlobalIngredient('1', Icon.LIGHT_CORNER_TOP_LEFT.item)
    Structure.addGlobalIngredient('2', Icon.LIGHT_CORNER_TOP_RIGHT.item)
    Structure.addGlobalIngredient('3', Icon.LIGHT_CORNER_BOTTOM_LEFT.item)
    Structure.addGlobalIngredient('4', Icon.LIGHT_CORNER_BOTTOM_RIGHT.item)
    Structure.addGlobalIngredient('5', Icon.LIGHT_VERTICAL_RIGHT.item)
    Structure.addGlobalIngredient('6', Icon.LIGHT_VERTICAL_LEFT.item)
    Structure.addGlobalIngredient('7', Icon.LIGHT_HORIZONTAL_UP.item)
    Structure.addGlobalIngredient('8', Icon.LIGHT_HORIZONTAL_DOWN.item)
    Structure.addGlobalIngredient('u', ::ScrollUpItem)
    Structure.addGlobalIngredient('d', ::ScrollDownItem)
}