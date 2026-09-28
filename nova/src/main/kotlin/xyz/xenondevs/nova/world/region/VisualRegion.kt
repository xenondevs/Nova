package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Vector3f
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.isGlowing
import xyz.xenondevs.nova.packetentity.packetItemDisplay
import xyz.xenondevs.nova.ui.menu.by
import xyz.xenondevs.nova.util.component1
import xyz.xenondevs.nova.util.component2
import xyz.xenondevs.nova.util.component3
import xyz.xenondevs.nova.util.component4
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
import xyz.xenondevs.nova.world.item.itemProvider
import java.awt.Color
import java.util.*

private const val MIN_LINE_WIDTH = 0.005
private const val MAX_LINE_WIDTH = 0.05
private const val DIAGONAL_THRESHOLD = 10.0

object VisualRegion {
    
    private val regions = HashMap<UUID, List<PacketItemDisplay>>()
    
    fun isVisible(player: Player, regionId: UUID) =
        regions[regionId]?.get(0)?.viewerWhitelist?.contains(player.uniqueId) ?: false
    
    fun toggleView(player: Player, regionId: UUID, region: Region) {
        if (isVisible(player, regionId)) {
            hideRegion(player, regionId)
        } else showRegion(player, regionId, region)
    }
    
    fun showRegion(player: Player, regionId: UUID, region: Region) {
        val outline = regions.getOrPut(regionId) { createOutline(regionId, region, emptySet()) }
        val newViewers = (outline[0].viewerWhitelist ?: emptySet()) + player.uniqueId
        outline.forEach { it.viewerWhitelist = newViewers }
    }
    
    fun hideRegion(player: Player, regionId: UUID) {
        val outline = regions[regionId] ?: return
        val currentViewers = outline[0].viewerWhitelist ?: emptySet()
        
        if (currentViewers.isEmpty() || currentViewers.size == 1 && currentViewers.first() == player.uniqueId) {
            removeRegion(regionId)
        } else {
            val newViewers = currentViewers + player.uniqueId
            outline.forEach { it.viewerWhitelist = newViewers }
        }
    }
    
    fun removeRegion(regionId: UUID) {
        val outline = regions.remove(regionId) ?: return
        outline.forEach { it.despawn() }
    }
    
    fun updateRegion(regionId: UUID, region: Region) {
        val outline = regions[regionId] ?: return
        val viewers = outline[0].viewerWhitelist ?: emptySet()
        removeRegion(regionId)
        regions[regionId] = createOutline(regionId, region, viewers)
    }
    
    private fun createOutline(regionId: UUID, region: Region, viewers: Set<UUID>): List<PacketItemDisplay> {
        val min = region.min
        val max = region.max
        val color = Color(regionId.hashCode()).rgb
        
        return getEdgeDisplays(min, max, color, viewers)
    }
    
    private fun getEdgeDisplays(min: Location, max: Location, color: Int, viewers: Set<UUID>): List<PacketItemDisplay> {
        val [world, minX, minY, minZ] = min
        val [_, maxX, maxY, maxZ] = max
        
        // linearly weighted line width between MIN_LINE_WIDTH and MAX_LINE_WIDTH, depending on the diagonal length of the region
        val lineWidth = (min.distance(max) / DIAGONAL_THRESHOLD).coerceIn(0.0, 1.0) * (MAX_LINE_WIDTH - MIN_LINE_WIDTH) + MIN_LINE_WIDTH
        
        fun createLine(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) =
            createLine(Location(world, x1, y1, z1), Location(world, x2, y2, z2), lineWidth, color, viewers)
        
        return listOf(
            // minX -> maxX
            createLine(minX, minY, minZ, maxX, minY, minZ),
            createLine(minX, minY, maxZ, maxX, minY, maxZ),
            createLine(minX, maxY, minZ, maxX, maxY, minZ),
            createLine(minX, maxY, maxZ, maxX, maxY, maxZ),
            // minY -> maxY
            createLine(minX, minY, minZ, minX, maxY, minZ),
            createLine(minX, minY, maxZ, minX, maxY, maxZ),
            createLine(maxX, minY, minZ, maxX, maxY, minZ),
            createLine(maxX, minY, maxZ, maxX, maxY, maxZ),
            // minZ -> maxZ
            createLine(minX, minY, minZ, minX, minY, maxZ),
            createLine(minX, maxY, minZ, minX, maxY, maxZ),
            createLine(maxX, minY, minZ, maxX, minY, maxZ),
            createLine(maxX, maxY, minZ, maxX, maxY, maxZ),
        )
    }
    
    private fun createLine(from: Location, to: Location, lineWidth: Double, color: Int, viewers: Set<UUID>) = packetItemDisplay {
        viewerWhitelist by viewers
        location by from.clone().add(to).multiply(0.5)
        metadata {
            itemStack by DefaultBlockOverlays.TRANSPARENT_BLOCK.itemProvider
            scale by Vector3f(
                (to.x - from.x + lineWidth).toFloat(),
                (to.y - from.y + lineWidth).toFloat(),
                (to.z - from.z + lineWidth).toFloat(),
            )
            isGlowing by true
            glowColorOverride by org.bukkit.Color.fromARGB(color)
        }
    }.apply { spawn() }
    
}
