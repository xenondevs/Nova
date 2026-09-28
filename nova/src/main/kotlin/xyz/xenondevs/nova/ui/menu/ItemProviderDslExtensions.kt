package xyz.xenondevs.nova.ui.menu

import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.dsl.DslProperty
import xyz.xenondevs.invui.dsl.ItemProviderDsl
import xyz.xenondevs.invui.dsl.itemProvider
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.registry.NovaItemBuilder
import xyz.xenondevs.nova.world.item.logic.PacketItems
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// Breaks server-side localization, but that's ok because Nova doesn't use it.
inline fun itemProvider(base: Provider<ItemProvider>, itemProvider: ItemProviderDsl.() -> Unit): Provider<ItemProvider> {
    contract { callsInPlace(itemProvider, InvocationKind.EXACTLY_ONCE) }
    return itemProvider(base.map(ItemProvider::get), itemProvider)
}

/**
 * Whether the item has Nova's advanced tooltip, which includes information like the item id.
 * - `true`: advanced tooltips are always shown
 * - `false`: advanced tooltips are never shown
 * - `null`: advanced tooltips are shown based on user preference (configurable via `/nova advancedTooltips`) except 
 * [NovaItemBuilder.hidden] items, which do not show advanced tooltips by default.
 */
context(dsl: ItemProviderDsl)
val advancedTooltips: DslProperty<Boolean?>
    get() = dsl.pdc[PacketItems.ADVANCED_TOOLTIP_OVERRIDE, PersistentDataType.BOOLEAN]