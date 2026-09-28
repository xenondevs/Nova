package xyz.xenondevs.nova.packetentity

import org.bukkit.Location

/**
 * Applies [update] to the metadata of every non-null packet entity in this iterable.
 */
inline fun <M : EntityMetadata> Iterable<PacketEntity<M>?>.updateMetadata(update: M.() -> Unit) =
    forEach { it?.metadata?.update() }

/**
 * Applies [update] to the equipment of every non-null packet entity in this iterable.
 */
inline fun Iterable<PacketEntity<*>?>.updateEquipment(update: PacketEntityEquipment.() -> Unit) =
    forEach { it?.equipment?.update() }

/**
 * Teleports every packet entity in this iterable by applying [modifyLocation] to each entity's
 * current location.
 */
fun Iterable<PacketEntity<*>>.teleport(modifyLocation: Location.() -> Unit) =
    forEach { it.teleport(modifyLocation) }

/**
 * Spawns every non-null packet entity in this iterable.
 */
fun Iterable<PacketEntity<*>?>.spawn() =
    forEach { it?.spawn() }

/**
 * Despawns every non-null packet entity in this iterable.
 */
fun Iterable<PacketEntity<*>?>.despawn() =
    forEach { it?.despawn() }

/**
 * Despawns every non-null packet entity in this collection and then clears the collection.
 */
fun MutableCollection<out PacketEntity<*>?>.clearAndDespawn() {
    despawn()
    clear()
}

/**
 * Removes and despawns every packet entity in this collection for which [filter] returns `true`.
 */
fun <M : EntityMetadata> MutableCollection<out PacketEntity<M>>.removeAndDespawnIf(filter: (PacketEntity<M>) -> Boolean) {
    removeIf {
        if (filter(it)) {
            it.despawn()
            true
        } else false
    }
}