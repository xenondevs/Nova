package xyz.xenondevs.nova.network.event

import org.bukkit.event.EventPriority

annotation class PacketHandler(
    val priority: EventPriority = EventPriority.NORMAL,
    val ignoreIfCancelled: Boolean = false
)