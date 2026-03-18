package xyz.xenondevs.nova.ui.menu

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.IngredientsDsl
import xyz.xenondevs.invui.dsl.InventoryWithBackgroundProvider
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.WindowDsl
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
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.map
import xyz.xenondevs.nova.ui.menu.item.PageBackItem
import xyz.xenondevs.nova.ui.menu.item.PageForwardItem
import xyz.xenondevs.nova.ui.menu.item.ScrollDownItem
import xyz.xenondevs.nova.ui.menu.item.ScrollUpItem
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.getTitle
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider
import java.util.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
inline fun itemProvider(base: Provider<ItemProvider>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.map(ItemProvider::get), itemProvider)
}

@JvmName("itemProviderNovaItem")
inline fun itemProvider(base: Provider<NovaItem>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.clientsideProvider, itemProvider)
}

fun itemProvider(base: RegistryEntry.Either<NovaItem, ItemType>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(
        base.map(
            { it.clientsideProvider.map(ItemProvider::get) },
            { provider(it.createItemStack()) }
        ).flatten(),
        itemProvider
    )
}

fun itemProvider(base: NovaItem, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.clientsideProvider, itemProvider)
}

@JvmName("by1")
context(dsl: ItemProviderDsl)
infix fun ProviderDslProperty<ItemType?>.by(type: Provider<Provider<NovaItem>>) {
    dsl.base by type.flatten().clientsideProvider.map(ItemProvider::get)
    dsl.type by null
}

context(dsl: ItemProviderDsl)
infix fun ProviderDslProperty<ItemType?>.by(type: Provider<NovaItem>) {
    dsl.base by type.clientsideProvider.map(ItemProvider::get)
    dsl.type by null
}

context(dsl: ItemProviderDsl)
infix fun ProviderDslProperty<ItemType>.by(type: RegistryEntry.Either<NovaItem, ItemType>) {
    dsl.base by type.map(
        { it.clientsideProvider.map(ItemProvider::get) },
        { provider(it.createItemStack()) }
    ).flatten()
    dsl.type by null
}

fun InventoryLink(inventory: Inventory, slot: Int, background: RegistryEntry.Nova<NovaItem>): SlotElement.InventoryLink =
    InventoryLink(inventory, slot, background.clientsideProvider)

context(dsl: IngredientsDsl)
infix fun Char.by(item: Provider<NovaItem>) {
    with(dsl) {
        this@by by item.clientsideProvider
    }
}

infix fun Inventory.with(item: Provider<NovaItem>) = 
    InventoryWithBackgroundProvider(this, item.clientsideProvider)

infix fun ProviderDslProperty<in ItemProvider>.by(novaItem: Provider<NovaItem>): Unit =
    by(novaItem.clientsideProvider)

@JvmName("by1")
infix fun ProviderDslProperty<in ItemProvider>.by(novaItem: Provider<Provider<NovaItem>>): Unit =
    by(novaItem.flatten().clientsideProvider)

context(dsl: WindowDsl)
infix fun ProviderDslProperty<Component>.by(guiTexture: RegistryEntry.Nova<GuiTexture>): Unit =
    by(guiTexture.getTitle(dsl.locale))

val WindowDsl.locale: Provider<Locale>
    get() = LocaleManager.getLocaleProvider(viewer)

internal fun Provider<NovaItem>.asUiItem(): Item =
    Item.builder().setItemProvider(clientsideProvider).build()