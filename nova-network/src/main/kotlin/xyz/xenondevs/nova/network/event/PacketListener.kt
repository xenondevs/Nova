package xyz.xenondevs.nova.network.event

/**
 * Marker interface for classes that are packet listeners.
 * After registering via [registerPacketListener], method annotated with [PacketHandler]
 * will be called for packet events, just like with Bukkit's event system.
 */
interface PacketListener

/**
 * Registers the [packet listener][listener] to receive packet events.
 * After registering, methods annotated with [PacketHandler] will be called for packet events.
 */
context(listener: PacketListener)
fun registerPacketListener() {
    PacketEventManager.registerListener(listener)
}

/**
 * Unregisters the [packet listener][listener] from receiving packet events.
 * After unregistering, methods annotated with [PacketHandler] will no longer be called for packet events.
 */
context(listener: PacketListener)
fun unregisterPacketListener() {
    PacketEventManager.unregisterListener(listener)
}