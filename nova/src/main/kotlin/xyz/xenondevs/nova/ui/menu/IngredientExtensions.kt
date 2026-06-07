package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.dsl.IngredientsDsl
import xyz.xenondevs.invui.dsl.InventoryWithBackgroundProvider
import xyz.xenondevs.invui.gui.IngredientMapper
import xyz.xenondevs.invui.gui.InventoryLink
import xyz.xenondevs.invui.gui.SlotElement
import xyz.xenondevs.invui.gui.addIngredient
import xyz.xenondevs.invui.inventory.Inventory
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, item: Provider<NovaItem>): S =
    addIngredient(char, item.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, item: NovaItem): S =
    addIngredient(char, item.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, inventory: Inventory, background: Provider<NovaItem>): S =
    addIngredient(char, inventory, background.clientsideProvider)

fun <S : IngredientMapper<S>> IngredientMapper<S>.addIngredient(char: Char, inventory: Inventory, background: NovaItem): S =
    addIngredient(char, inventory, background.clientsideProvider)

context(dsl: IngredientsDsl)
infix fun Char.by(item: Provider<NovaItem>) {
    with(dsl) {
        this@by by item.clientsideProvider
    }
}

fun InventoryLink(inventory: Inventory, slot: Int, background: RegistryEntry.Nova<NovaItem>): SlotElement.InventoryLink =
    InventoryLink(inventory, slot, background.clientsideProvider)

infix fun Inventory.with(item: Provider<NovaItem>) =
    InventoryWithBackgroundProvider(this, item.clientsideProvider)