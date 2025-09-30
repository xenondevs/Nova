package xyz.xenondevs.nova.ui.menu.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.BundleContents.bundleContents
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.internal.util.ItemUtils2
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import kotlin.random.Random

private const val SLOTS = 3
private const val FIRST_SLOT = 0
private const val LAST_SLOT = SLOTS - 1

/**
 * Creates a scroller UI item that can be used to scroll through a [xyz.xenondevs.invui.gui.ScrollGui]
 * using the mouse wheel.
 */
@Suppress("AssignedValueIsNeverRead") // broken inspection
fun scrollerItem(
    serverWindowState: MutableProvider<Int>,
    clientWindowState: Provider<Int>,
    line: MutableProvider<Int>,
    maxLine: Provider<Int>,
    itemProvider: Provider<ItemProvider> = provider(DefaultGuiItems.TP_SCROLLER_VERTICAL.clientsideProvider),
): Item = item {
    this.itemProvider by combinedProvider(line, maxLine, itemProvider) { line, maxLine, itemProvider ->
        val progress = (line + 1.0) / (maxLine + 1.0)
        ItemBuilder(ItemUtils2.asType(itemProvider.get(), Material.BUNDLE))
            .set(DataComponentTypes.BUNDLE_CONTENTS, bundleContents(listOf(
                ItemType.STONE.createItemStack(((progress * 64).toInt() - 2).coerceIn(1..64)),
                ItemType.STONE.createItemStack(),
                ItemType.STONE.createItemStack()
            )))
            .hideTooltip(true)
            // set random component to make sure the bundle resets every time serverWindowState is updated
            .set(DataComponentTypes.BREAK_SOUND, Key.key(Random.nextInt().toString()))
    }
    
    var prevState = 0
    var prevSlot = -1
    onBundleSelect {
        val cws = clientWindowState.get()
        if (prevState != cws) {
            prevSlot = -1
            prevState = cws
        }
        
        when (determineScrollDirection(prevSlot, bundleSlot)) {
            1 if line.get() > 0 -> {
                line.set(line.get() - 1)
                serverWindowState.set(serverWindowState.get() + 1)
            }
            
            -1 if line.get() < maxLine.get() -> {
                line.set(line.get() + 1)
                serverWindowState.set(serverWindowState.get() + 1)
            }
            
            else -> Unit
        }
        
        prevSlot = bundleSlot
    }
}

/**
 * Returns the scroll direction the scroll wheel turned when moving from the `from` slot to the `to` bundle slot,
 * where `1` means up, `-1` means down and `0` means no or unknown movement.
 */
private fun determineScrollDirection(from: Int, to: Int): Int = when (to) {
    -1 -> 0 // release
    FIRST_SLOT if from == -1 -> -1
    LAST_SLOT if from == -1 -> 1
    (from + 1).mod(SLOTS) -> -1
    (from - 1).mod(SLOTS) -> 1
    else -> 0 // illegal transition
}