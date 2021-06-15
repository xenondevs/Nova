package xyz.xenondevs.nova.region

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.xenondevs.nova.util.createColoredParticle
import xyz.xenondevs.nova.util.getBoxOutline
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.particle.utils.ParticleUtils
import java.awt.Color
import java.util.*

object VisualRegion {
    
    private val regions = HashMap<UUID, Pair<MutableList<UUID>, List<Any>>>()
    
    fun init() {
        runTaskTimer(0, 3, ::handleTick)
    }
    
    fun showRegion(player: Player, regionUUID: UUID, min: Location, max: Location) {
        getViewerList(regionUUID, min, max).add(player.uniqueId)
    }
    
    fun toggleView(player: Player, regionUUID: UUID, pos1: Location, pos2: Location) {
        val viewerList = getViewerList(regionUUID, pos1, pos2)
        if (viewerList.contains(player.uniqueId)) {
            viewerList.remove(player.uniqueId)
            if (viewerList.isEmpty()) regions.remove(regionUUID)
        } else {
            viewerList.add(player.uniqueId)
        }
    }
    
    fun isVisible(player: Player, regionUUID: UUID) =
        isVisible(player.uniqueId, regionUUID)
    
    fun isVisible(playerUUID: UUID, regionUUID: UUID) =
        regions[regionUUID]?.first?.contains(playerUUID) ?: false
    
    fun removeRegionViewer(player: Player, regionUUID: UUID) {
        val viewerList = regions[regionUUID]?.first
        if (viewerList != null) {
            viewerList.remove(player.uniqueId)
            if (viewerList.isEmpty()) removeRegion(regionUUID)
        }
    }
    
    fun removeRegion(regionUUID: UUID) {
        regions.remove(regionUUID)
    }
    
    private fun getViewerList(regionUUID: UUID, pos1: Location, pos2: Location) =
        regions[regionUUID]?.first
            ?: ArrayList<UUID>().also { regions[regionUUID] = it to getParticlePackets(regionUUID, pos1, pos2) }
    
    private fun getParticlePackets(regionUUID: UUID, pos1: Location, pos2: Location): List<Any> {
        val color = Color(regionUUID.hashCode())
        return pos1.getBoxOutline(pos2, false).map { it.createColoredParticle(color) }
    }
    
    private fun handleTick() {
        regions.forEach { _, (viewerList, particlePackets) ->
            val playerList = viewerList.mapNotNull(Bukkit::getPlayer)
            ParticleUtils.sendBulk(particlePackets, playerList)
        }
    }
}
