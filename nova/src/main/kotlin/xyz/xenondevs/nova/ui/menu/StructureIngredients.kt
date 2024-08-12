package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.gui.structure.Structure
import xyz.xenondevs.invui.gui.structure.Structure.addGlobalIngredient
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.nova.ui.menu.item.PageBackItem
import xyz.xenondevs.nova.ui.menu.item.PageForwardItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', DefaultGuiItems.INVENTORY_PART.model.clientsideProvider)
    addGlobalIngredient('-', DefaultGuiItems.LINE_HORIZONTAL.model.clientsideProvider)
    addGlobalIngredient('|', DefaultGuiItems.LINE_VERTICAL.model.clientsideProvider)
    addGlobalIngredient('1', DefaultGuiItems.LINE_CORNER_TOP_LEFT.model.clientsideProvider)
    addGlobalIngredient('2', DefaultGuiItems.LINE_CORNER_TOP_RIGHT.model.clientsideProvider)
    addGlobalIngredient('3', DefaultGuiItems.LINE_CORNER_BOTTOM_LEFT.model.clientsideProvider)
    addGlobalIngredient('4', DefaultGuiItems.LINE_CORNER_BOTTOM_RIGHT.model.clientsideProvider)
    addGlobalIngredient('5', DefaultGuiItems.LINE_VERTICAL_RIGHT.model.clientsideProvider)
    addGlobalIngredient('6', DefaultGuiItems.LINE_VERTICAL_LEFT.model.clientsideProvider)
    addGlobalIngredient('7', DefaultGuiItems.LINE_HORIZONTAL_UP.model.clientsideProvider)
    addGlobalIngredient('8', DefaultGuiItems.LINE_HORIZONTAL_DOWN.model.clientsideProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}

fun Structure.addIngredient(char: Char, item: NovaItem) =
    addIngredient(char, item.model.clientsideProvider)

fun <G : Gui, B : Gui.Builder<G, B>> Gui.Builder<G, B>.addIngredient(char: Char, item: NovaItem) =
    addIngredient(char, item.model.clientsideProvider)

fun Structure.addIngredient(char: Char, inventory: Inventory, background: NovaItem) =
    addIngredient(char, inventory, background.model.clientsideProvider)

fun <G : Gui, B : Gui.Builder<G, B>> Gui.Builder<G, B>.addIngredient(char: Char, inventory: Inventory, background: NovaItem) =
    addIngredient(char, inventory, background.model.clientsideProvider)

internal fun <G : Gui, B : Gui.Builder<G, B>> B.applyDefaultTPIngredients(): B {
    addIngredient('u', ScrollUpItem(
        on = DefaultGuiItems.TP_ARROW_UP_ON.model.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_UP_OFF.model.clientsideProvider
    ))
    addIngredient('d', ScrollDownItem(
        on = DefaultGuiItems.TP_ARROW_DOWN_ON.model.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_DOWN_OFF.model.clientsideProvider
    ))
    addIngredient('<', PageBackItem(
        on = DefaultGuiItems.TP_ARROW_LEFT_ON.model.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_LEFT_OFF.model.clientsideProvider
    ))
    addIngredient('>', PageForwardItem(
        on = DefaultGuiItems.TP_ARROW_RIGHT_ON.model.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_RIGHT_OFF.model.clientsideProvider
    ))
    
    return this
}