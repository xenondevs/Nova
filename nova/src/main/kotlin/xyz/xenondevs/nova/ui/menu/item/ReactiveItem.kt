package xyz.xenondevs.nova.ui.menu.item

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.impl.AbstractItem

private typealias ClickHandler = (clickType: ClickType, player: Player, event: InventoryClickEvent) -> Unit

private val EMPTY_CLICK_HANDLER: ClickHandler = { _, _, _ -> }

fun <A> reactiveItem(
    a: Provider<A>,
    mapValue: (A) -> ItemProvider
): Item = ReactiveItem(a.map(mapValue), EMPTY_CLICK_HANDLER)

fun <A> reactiveItem(
    a: Provider<A>,
    mapValue: (A) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(a.map(mapValue), clickHandler)

fun <A, B> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    mapValue: (A, B) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B> combinedProvider(
    a: Provider<A>,
    b: Provider<B>,
    mapValue: (A, B) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, mapValue), clickHandler)

fun <A, B, C> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    mapValue: (A, B, C) -> ItemProvider
): Item = ReactiveItem(combinedProvider(a, b, c, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    mapValue: (A, B, C) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, mapValue), clickHandler)

fun <A, B, C, D> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    mapValue: (A, B, C, D) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    mapValue: (A, B, C, D) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, mapValue), clickHandler)

fun <A, B, C, D, E> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    mapValue: (A, B, C, D, E) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    mapValue: (A, B, C, D, E) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, mapValue), clickHandler)

fun <A, B, C, D, E, F> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    mapValue: (A, B, C, D, E, F) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E, F> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    mapValue: (A, B, C, D, E, F) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, mapValue), clickHandler)

fun <A, B, C, D, E, F, G> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    mapValue: (A, B, C, D, E, F, G) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E, F, G> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    mapValue: (A, B, C, D, E, F, G) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, mapValue), clickHandler)

fun <A, B, C, D, E, F, G, H> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    mapValue: (A, B, C, D, E, F, G, H) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E, F, G, H> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    mapValue: (A, B, C, D, E, F, G, H) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, mapValue), clickHandler)

fun <A, B, C, D, E, F, G, H, I> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    i: Provider<I>,
    mapValue: (A, B, C, D, E, F, G, H, I) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, i, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E, F, G, H, I> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    i: Provider<I>,
    mapValue: (A, B, C, D, E, F, G, H, I) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, i, mapValue), clickHandler)

fun <A, B, C, D, E, F, G, H, I, J> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    i: Provider<I>,
    j: Provider<J>,
    mapValue: (A, B, C, D, E, F, G, H, I, J) -> ItemProvider,
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, i, j, mapValue), EMPTY_CLICK_HANDLER)

fun <A, B, C, D, E, F, G, H, I, J> reactiveItem(
    a: Provider<A>,
    b: Provider<B>,
    c: Provider<C>,
    d: Provider<D>,
    e: Provider<E>,
    f: Provider<F>,
    g: Provider<G>,
    h: Provider<H>,
    i: Provider<I>,
    j: Provider<J>,
    mapValue: (A, B, C, D, E, F, G, H, I, J) -> ItemProvider,
    clickHandler: ClickHandler
): Item = ReactiveItem(combinedProvider(a, b, c, d, e, f, g, h, i, j, mapValue), clickHandler)

private class ReactiveItem(
    val provider: Provider<ItemProvider>,
    val clickHandler: (clickType: ClickType, player: Player, event: InventoryClickEvent) -> Unit = { _, _, _ -> }
) : AbstractItem() {
    
    init {
        provider.subscribeWeak(this) { item, _ -> item.notifyWindows() }
    }
    
    override fun getItemProvider(): ItemProvider = provider.get()
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = clickHandler(clickType, player, event)
    
}