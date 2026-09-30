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

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("this == Action.LEFT_CLICK_BLOCK || this == Action.RIGHT_CLICK_BLOCK", "org.bukkit.event.block.Action"))
fun Action.isClickBlock() = this == Action.LEFT_CLICK_BLOCK || this == Action.RIGHT_CLICK_BLOCK

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("this == Action.LEFT_CLICK_AIR || this == Action.RIGHT_CLICK_AIR", "org.bukkit.event.block.Action"))
fun Action.isClickAir() = this == Action.LEFT_CLICK_AIR || this == Action.RIGHT_CLICK_AIR

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY", "org.bukkit.event.Event.Result"))
fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)"))
val PlayerInteractEvent.handItems: Array<ItemStack>
    get() = arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)

@Deprecated(DEBLOAT_DEPRECATE, ReplaceWith("arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)", "org.bukkit.inventory.EquipmentSlot"))
val PlayerInteractEvent.hands: Array<Pair<EquipmentSlot, ItemStack>>
    get() = arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)

/**
 * Shortcut for [PluginManager.callEvent].
 */
fun callEvent(event: Event) {
    Bukkit.getPluginManager().callEvent(event)
}

/**
 * Shortcut for registering [listener] under the Nova plugin via [PluginManager.registerEvents].
 */
context(listener: Listener)
fun registerEvents() {
    check(PLUGIN_READY) { "Events cannot be registered this early! Use a post-world initialization stage for this." }
    Bukkit.getPluginManager().registerEvents(listener, Nova)
}

/**
 * Unregisters the [listener] from all events.
 */
context(listener: Listener)
fun unregisterEvents() {
    HandlerList.unregisterAll(listener)
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