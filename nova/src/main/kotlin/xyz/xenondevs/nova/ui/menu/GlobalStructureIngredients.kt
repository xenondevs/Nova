@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.invui.dsl.by
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.Structure
import xyz.xenondevs.invui.gui.Structure.addGlobalIngredient
import xyz.xenondevs.nova.ui.menu.item.PageBackItem
import xyz.xenondevs.nova.ui.menu.item.PageForwardItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems

internal const val LEGACY_NON_DSL_INVUI_DEPRECATION = "A replacement UI component for InvUI's DSL API is available. " +
    "The DSL API should be preferred as it works better with reactive features such as config- and registry reloading. " +
    "The replacement UI components can typically be found as lowerCamelCase functions, e.g. `BackItem(...)` -> `backItem(...)`. " +
    "This legacy UI component will be removed in a future Nova version."

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('.', item { itemProvider by DefaultGuiItems.INVISIBLE_ITEM })
    addGlobalIngredient('#', item { itemProvider by DefaultGuiItems.INVENTORY_PART })
    addGlobalIngredient('-', item { itemProvider by DefaultGuiItems.LINE_HORIZONTAL })
    addGlobalIngredient('|', item { itemProvider by DefaultGuiItems.LINE_VERTICAL })
    addGlobalIngredient('1', item { itemProvider by DefaultGuiItems.LINE_CORNER_TOP_LEFT })
    addGlobalIngredient('2', item { itemProvider by DefaultGuiItems.LINE_CORNER_TOP_RIGHT })
    addGlobalIngredient('3', item { itemProvider by DefaultGuiItems.LINE_CORNER_BOTTOM_LEFT })
    addGlobalIngredient('4', item { itemProvider by DefaultGuiItems.LINE_CORNER_BOTTOM_RIGHT })
    addGlobalIngredient('5', item { itemProvider by DefaultGuiItems.LINE_VERTICAL_RIGHT })
    addGlobalIngredient('6', item { itemProvider by DefaultGuiItems.LINE_VERTICAL_LEFT })
    addGlobalIngredient('7', item { itemProvider by DefaultGuiItems.LINE_HORIZONTAL_UP })
    addGlobalIngredient('8', item { itemProvider by DefaultGuiItems.LINE_HORIZONTAL_DOWN })
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
    Structure.freezeGlobalIngredients()
}