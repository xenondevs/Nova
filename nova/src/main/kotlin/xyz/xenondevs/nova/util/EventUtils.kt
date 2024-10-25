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
import xyz.xenondevs.nova.patch.impl.misc.EventPreventionPatch

fun Action.isClickBlock() = this == Action.LEFT_CLICK_BLOCK || this == Action.RIGHT_CLICK_BLOCK

fun Action.isClickAir() = this == Action.LEFT_CLICK_AIR || this == Action.RIGHT_CLICK_AIR

fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY

val PlayerInteractEvent.handItems: Array<ItemStack>
    get() = arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)

val PlayerInteractEvent.hands: Array<Pair<EquipmentSlot, ItemStack>>
    get() = arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)

fun callEvent(event: Event) = Bukkit.getPluginManager().callEvent(event)

/**
 * Shortcut for [PluginManager.registerEvents], registered under the Nova plugin.
 */
fun Listener.registerEvents() {
    Bukkit.getPluginManager().registerEvents(this, Nova)
}

/**
 * Unregisters this [Listener] from all events.
 */
fun Listener.unregisterEvents() {
    HandlerList.unregisterAll(this)
}

/**
 * Prevents all [synchronous][Event.async] [events][Event] attempted to be fired during [run]
 * from being fired.
 */
inline fun preventEvents(run: () -> Unit) {
    EventPreventionPatch.dropAll = true
    try {
        run()
    } finally {
        EventPreventionPatch.dropAll = false
    }
}