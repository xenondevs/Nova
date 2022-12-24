package xyz.xenondevs.nova.tileentity

import net.minecraft.network.protocol.Packet
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send

class TileEntityPacketTask(
    private val tileEntity: TileEntity,
    private val packets: List<Packet<*>>,
    private val interval: Long
) {
    
    private var task: BukkitTask? = null
    
    fun start() {
        if (task == null)
            task = runTaskTimer(0, interval, ::sendPackets)
    }
    
    fun stop() {
        task?.cancel()
        task = null
    }
    
    private fun sendPackets() {
        tileEntity.getViewers().forEach { it.send(packets) }
    }
    
    fun isRunning() = task != null
    
}