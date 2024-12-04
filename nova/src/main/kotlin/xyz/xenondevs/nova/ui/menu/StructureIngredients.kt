package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.IngredientMapper
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.Structure.addGlobalIngredient
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.nova.ui.menu.item.PageBackItem
import xyz.xenondevs.nova.ui.menu.item.PageForwardItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('#', DefaultGuiItems.INVENTORY_PART.clientsideProvider)
    addGlobalIngredient('-', DefaultGuiItems.LINE_HORIZONTAL.clientsideProvider)
    addGlobalIngredient('|', DefaultGuiItems.LINE_VERTICAL.clientsideProvider)
    addGlobalIngredient('1', DefaultGuiItems.LINE_CORNER_TOP_LEFT.clientsideProvider)
    addGlobalIngredient('2', DefaultGuiItems.LINE_CORNER_TOP_RIGHT.clientsideProvider)
    addGlobalIngredient('3', DefaultGuiItems.LINE_CORNER_BOTTOM_LEFT.clientsideProvider)
    addGlobalIngredient('4', DefaultGuiItems.LINE_CORNER_BOTTOM_RIGHT.clientsideProvider)
    addGlobalIngredient('5', DefaultGuiItems.LINE_VERTICAL_RIGHT.clientsideProvider)
    addGlobalIngredient('6', DefaultGuiItems.LINE_VERTICAL_LEFT.clientsideProvider)
    addGlobalIngredient('7', DefaultGuiItems.LINE_HORIZONTAL_UP.clientsideProvider)
    addGlobalIngredient('8', DefaultGuiItems.LINE_HORIZONTAL_DOWN.clientsideProvider)
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
}

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, item: NovaItem): S =
    addIngredient(char, item.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, inventory: Inventory, background: NovaItem): S =
    addIngredient(char, inventory, background.clientsideProvider)

internal fun <G : Gui, B : Gui.Builder<G, B>> B.applyDefaultTPIngredients(): B {
    addIngredient('u', ScrollUpItem(
        on = DefaultGuiItems.TP_ARROW_UP_ON.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_UP_OFF.clientsideProvider
    ))
    addIngredient('d', ScrollDownItem(
        on = DefaultGuiItems.TP_ARROW_DOWN_ON.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_DOWN_OFF.clientsideProvider
    ))
    addIngredient('<', PageBackItem(
        on = DefaultGuiItems.TP_ARROW_LEFT_ON.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_LEFT_OFF.clientsideProvider
    ))
    addIngredient('>', PageForwardItem(
        on = DefaultGuiItems.TP_ARROW_RIGHT_ON.clientsideProvider,
        off = DefaultGuiItems.TP_ARROW_RIGHT_OFF.clientsideProvider
    ))
    
    return this
}