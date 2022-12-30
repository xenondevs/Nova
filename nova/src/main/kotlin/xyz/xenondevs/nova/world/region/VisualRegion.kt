package xyz.xenondevs.nova.world.region

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.util.createColoredParticle
import xyz.xenondevs.nova.util.getBoxOutline
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.send
import java.awt.Color
import java.util.*

object VisualRegion {
    
    private val tasks = HashMap<UUID, BukkitTask>()
    private val viewers = HashMap<UUID, MutableList<UUID>>()
    
    fun showRegion(player: Player, regionUUID: UUID, region: Region) {
        getViewerList(regionUUID, region).add(player.uniqueId)
    }
    
    fun toggleView(player: Player, regionUUID: UUID, region: Region) {
        val viewerList = getViewerList(regionUUID, region)
        if (viewerList.contains(player.uniqueId)) {
            viewerList.remove(player.uniqueId)
            if (viewerList.isEmpty()) removeRegion(regionUUID)
        } else viewerList.add(player.uniqueId)
    }
    
    fun isVisible(player: Player, regionUUID: UUID) =
        isVisible(player.uniqueId, regionUUID)
    
    fun isVisible(playerUUID: UUID, regionUUID: UUID) =
        viewers[regionUUID]?.contains(playerUUID) ?: false
    
    fun hideRegion(player: Player, regionUUID: UUID) {
        val viewerList = viewers[regionUUID]
        if (viewerList != null) {
            viewerList.remove(player.uniqueId)
            if (viewerList.isEmpty()) removeRegion(regionUUID)
        }
    }
    
    fun removeRegion(regionUUID: UUID) {
        tasks.remove(regionUUID)?.cancel()
        viewers.remove(regionUUID)
    }
    
    fun updateRegion(regionUUID: UUID, region: Region) {
        val task = tasks[regionUUID]
        if (task != null) {
            task.cancel()
            startShowingRegion(regionUUID, region)
        }
    }
    
    private fun getViewerList(regionUUID: UUID, region: Region): MutableList<UUID> {
        if (regionUUID in viewers)
            return viewers[regionUUID]!!
        startShowingRegion(regionUUID, region)
        return viewers[regionUUID]!!
    }
    
    private fun startShowingRegion(regionUUID: UUID, region: Region) {
        val packets = getParticlePackets(regionUUID, region)
        viewers.computeIfAbsent(regionUUID) { ArrayList() }
        tasks[regionUUID] = runTaskTimer(0, 3) {
            viewers[regionUUID]
                ?.asSequence()
                ?.mapNotNull(Bukkit::getPlayer)
                ?.filter { it.location.world == region.world }
                ?.forEach { it.send(packets) }
        }
    }
    
    private fun getParticlePackets(regionUUID: UUID, region: Region): List<ClientboundLevelParticlesPacket> {
        val color = Color(regionUUID.hashCode())
        return region.min.getBoxOutline(region.max, false).map { it.createColoredParticle(color) }
    }
    
}
