package xyz.xenondevs.nova.tileentity

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send

// TODO: move to different package
class PacketTask(
    private val packets: List<Packet<*>>,
    private val interval: Long,
    private val getViewers: () -> Iterable<Player>
): AutoCloseable {
    
    private var task: BukkitTask? = null
    
    fun start() {
        if (task == null)
            task = runTaskTimer(0, interval, ::sendPackets)
    }
    
    fun stop() {
        task?.cancel()
        task = null
    }
    
    override fun close() {
        stop()
    }
    
    private fun sendPackets() {
        getViewers().forEach { it.send(packets) }
    }
    
    fun isRunning() = task != null
    
}