@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.Event.Result
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.PluginManager
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.PLUGIN_READY

fun Action.isClickBlock() = this == Action.LEFT_CLICK_BLOCK || this == Action.RIGHT_CLICK_BLOCK

fun Action.isClickAir() = this == Action.LEFT_CLICK_AIR || this == Action.RIGHT_CLICK_AIR

fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY

val PlayerInteractEvent.handItems: Array<ItemStack>
    get() = arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)

val PlayerInteractEvent.hands: Array<Pair<EquipmentSlot, ItemStack>>
    get() = arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)

/**
 * Shortcut for [PluginManager.callEvent].
 */
fun callEvent(event: Event) {
    Bukkit.getPluginManager().callEvent(event)
}

/**
 * Shortcut for [PluginManager.registerEvents], registered under the Nova plugin.
 */
fun Listener.registerEvents() {
    check(PLUGIN_READY) { "Events cannot be registered this early! Use a post-world initialization stage for this." }
    Bukkit.getPluginManager().registerEvents(this, Nova)
}

/**
 * Unregisters this [Listener] from all events.
 */
fun Listener.unregisterEvents() {
    HandlerList.unregisterAll(this)
}

/**
 * Blocks bukkit event firing during [run].
 */
inline fun preventEvents(run: () -> Unit) {
    EventUtils.dropAllEvents.set(true)
    try {
        run()
    } finally {
        EventUtils.dropAllEvents.set(false)
    }
}

@PublishedApi
internal object EventUtils {
    
    @JvmField
    val dropAllEvents: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    
}