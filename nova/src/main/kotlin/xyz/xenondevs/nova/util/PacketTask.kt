package xyz.xenondevs.nova.util

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * A task that sends the given [packets] to all players from [getViewers]
 * every [interval] ticks.
 */
class PacketTask(
    private val packets: List<Packet<*>>,
    private val interval: Long,
    private val getViewers: () -> Iterable<Player>
) {
    
    private var task: BukkitTask? = null
    
    /**
     * Creates a new [PacketTask] that sends the given [packet] to all players from [getViewers].
     */
    constructor(packet: Packet<*>, interval: Long, getViewers: () -> Iterable<Player>) : this(listOf(packet), interval, getViewers)
    
    /**
     * Starts the task.
     */
    fun start() {
        if (task == null)
            task = runTaskTimer(0, interval, ::sendPackets)
    }
    
    /**
     * Stops the task.
     */
    fun stop() {
        task?.cancel()
        task = null
    }
    
    /**
     * Checks whether the task is running.
     */
    fun isRunning(): Boolean = task != null
    
    private fun sendPackets() {
        getViewers().forEach { it.send(packets) }
    }
    
}