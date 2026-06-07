package xyz.xenondevs.nova.ui.menu

import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.dsl.DslProperty
import xyz.xenondevs.commons.provider.immediateFlatten
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.map
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.clientsideProvider
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// Breaks server-side localization, but that's ok because Nova doesn't use it.
inline fun itemProvider(base: Provider<ItemProvider>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.map(ItemProvider::get), itemProvider)
}

@JvmName("itemProvider1")
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
        ).immediateFlatten(),
        itemProvider
    )
}

fun itemProvider(base: NovaItem, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.clientsideProvider, itemProvider)
}

context(dsl: ItemProviderDsl)
infix fun DslProperty<ItemType?>.by(type: Provider<NovaItem>) {
    dsl.base by type.clientsideProvider.map(ItemProvider::get)
    dsl.type by null
}

context(dsl: ItemProviderDsl)
infix fun DslProperty<ItemType>.by(type: RegistryEntry.Either<NovaItem, ItemType>) {
    dsl.base by type.map(
        { it.clientsideProvider.map(ItemProvider::get) },
        { provider(it.createItemStack()) }
    ).immediateFlatten()
    dsl.type by null
}