package xyz.xenondevs.nova.network.event

import org.bukkit.event.EventPriority

/**
 * Annotate a function with a single parameter of type [PacketEvent] with this function
 * to create an event handler for a packet.
 * Requires implementing [PacketListener] and registration via [registerPacketListener].
 */
@Target(AnnotationTarget.FUNCTION)
annotation class PacketHandler(
    /**
     * The priority of this event handler.
     * Handles with higher priority will be called last,
     * and thus be able to override the results of handlers with lower priority.
     */
    val priority: EventPriority = EventPriority.NORMAL,
    /**
     * Whether this handler should be skipped if a previous handler already canceled the event.
     */
    val ignoreIfCancelled: Boolean = false
)