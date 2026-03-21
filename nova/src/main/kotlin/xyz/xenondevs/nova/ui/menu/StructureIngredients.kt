package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.dsl.IngredientsDsl
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.dsl.property.ProviderDslProperty
import xyz.xenondevs.invui.gui.IngredientMapper
import xyz.xenondevs.invui.gui.InventoryLink
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.Structure
import xyz.xenondevs.invui.gui.Structure.addGlobalIngredient
import xyz.xenondevs.invui.gui.addIngredient
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.setItemProvider
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.ui.menu.item.PageBackItem
import xyz.xenondevs.nova.ui.menu.item.PageForwardItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider

internal fun setGlobalIngredients() {
    addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
    addGlobalIngredient('.', DefaultGuiItems.INVISIBLE_ITEM.asUiItem())
    addGlobalIngredient('#', DefaultGuiItems.INVENTORY_PART.asUiItem())
    addGlobalIngredient('-', DefaultGuiItems.LINE_HORIZONTAL.asUiItem())
    addGlobalIngredient('|', DefaultGuiItems.LINE_VERTICAL.asUiItem())
    addGlobalIngredient('1', DefaultGuiItems.LINE_CORNER_TOP_LEFT.asUiItem())
    addGlobalIngredient('2', DefaultGuiItems.LINE_CORNER_TOP_RIGHT.asUiItem())
    addGlobalIngredient('3', DefaultGuiItems.LINE_CORNER_BOTTOM_LEFT.asUiItem())
    addGlobalIngredient('4', DefaultGuiItems.LINE_CORNER_BOTTOM_RIGHT.asUiItem())
    addGlobalIngredient('5', DefaultGuiItems.LINE_VERTICAL_RIGHT.asUiItem())
    addGlobalIngredient('6', DefaultGuiItems.LINE_VERTICAL_LEFT.asUiItem())
    addGlobalIngredient('7', DefaultGuiItems.LINE_HORIZONTAL_UP.asUiItem())
    addGlobalIngredient('8', DefaultGuiItems.LINE_HORIZONTAL_DOWN.asUiItem())
    addGlobalIngredient('u', ::ScrollUpItem)
    addGlobalIngredient('d', ::ScrollDownItem)
    addGlobalIngredient('<', ::PageBackItem)
    addGlobalIngredient('>', ::PageForwardItem)
    Structure.freezeGlobalIngredients()
}

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, item: Provider<NovaItem>): S =
    addIngredient(char, item.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, item: NovaItem): S =
    addIngredient(char, item.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, inventory: Inventory, background: Provider<NovaItem>): S =
    addIngredient(char, inventory, background.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, inventory: Inventory, background: NovaItem): S =
    addIngredient(char, inventory, background.clientsideProvider)

// Breaks server-side localization, but that's ok because Nova doesn't use it.
fun itemProvider(base: Provider<NovaItem>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> =
    itemProvider(base.clientsideProvider.map { it.get() }, itemProvider)

fun itemProvider(base: NovaItem, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> =
    itemProvider(base.clientsideProvider.map { it.get() }, itemProvider)

fun InventoryLink(inventory: Inventory, slot: Int, background: RegistryEntry.Nova<NovaItem>): SlotElement.InventoryLink =
    InventoryLink(inventory, slot, background.clientsideProvider)

context(dsl: IngredientsDsl)
infix fun Char.by(item: Provider<NovaItem>) {
    with(dsl) {
        this@by by item.clientsideProvider
    }
}

infix fun ProviderDslProperty<in ItemProvider>.by(novaItem: Provider<NovaItem>): Unit =
    by(novaItem.clientsideProvider)

internal fun Provider<NovaItem>.asUiItem(): Item =
    Item.builder().setItemProvider(clientsideProvider).build()