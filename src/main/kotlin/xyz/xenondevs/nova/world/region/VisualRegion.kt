package xyz.xenondevs.nova.world.region

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import xyz.xenondevs.nova.util.createColoredParticle
import xyz.xenondevs.nova.util.getBoxOutline
import xyz.xenondevs.particle.task.TaskManager
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList

object VisualRegion {
    
    private val taskId = HashMap<UUID, Int>()
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
    
    fun removeRegionViewer(player: Player, regionUUID: UUID) {
        val viewerList = viewers[regionUUID]
        if (viewerList != null) {
            viewerList.remove(player.uniqueId)
            if (viewerList.isEmpty()) removeRegion(regionUUID)
        }
    }
    
    fun removeRegion(regionUUID: UUID) {
        if (regionUUID !in taskId)
            return
        TaskManager.getTaskManager().stopTask(taskId[regionUUID]!!)
        taskId.remove(regionUUID)
        viewers.remove(regionUUID)
    }
    
    fun updateRegion(regionUUID: UUID, region: Region) {
        if (regionUUID in taskId) 
            TaskManager.getTaskManager().stopTask(taskId[regionUUID]!!)
        
        startShowingRegion(regionUUID, region)
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
        val id = TaskManager.startSuppliedTask(packets, 3) {
            viewers[regionUUID]!!
                .mapNotNull(Bukkit::getPlayer)
                .filter { it.location.world == region.world }
        }
        taskId[regionUUID] = id
    }
    
    private fun getParticlePackets(regionUUID: UUID, region: Region): List<Any> {
        val color = Color(regionUUID.hashCode())
        return region.min.getBoxOutline(region.max, false).map { it.createColoredParticle(color) }
    }
    
}
