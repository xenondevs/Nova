package xyz.xenondevs.nova.world.region

import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Vector3f
import xyz.xenondevs.nova.item.DefaultBlockOverlays
import xyz.xenondevs.nova.util.component1
import xyz.xenondevs.nova.util.component2
import xyz.xenondevs.nova.util.component3
import xyz.xenondevs.nova.util.component4
import xyz.xenondevs.nova.util.nmsCopy
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import java.awt.Color
import java.util.*

private fun Iterable<FakeItemDisplay>.spawn(viewer: Player) = forEach { it.spawn(viewer) }
private fun Iterable<FakeItemDisplay>.spawn(viewers: Iterable<Player>) = forEach { display -> viewers.forEach { display.spawn(it) } }
private fun Iterable<FakeItemDisplay>.despawn(viewer: Player) = forEach { it.despawn(viewer) }
private fun Iterable<FakeItemDisplay>.despawn(viewers: Iterable<Player>) = forEach { display -> viewers.forEach { display.despawn(it) } }

private const val MIN_LINE_WIDTH = 0.005
private const val MAX_LINE_WIDTH = 0.05
private const val DIAGONAL_THRESHOLD = 10.0

object VisualRegion {
    
    private val regions = HashMap<UUID, Pair<List<FakeItemDisplay>, MutableSet<Player>>>()
    
    fun isVisible(player: Player, regionId: UUID) =
        regions[regionId]?.second?.contains(player) ?: false
    
    fun toggleView(player: Player, regionId: UUID, region: Region) {
        if (isVisible(player, regionId)) {
            hideRegion(player, regionId)
        } else showRegion(player, regionId, region)
    }
    
    fun showRegion(player: Player, regionId: UUID, region: Region) {
        val (outline, viewers) = getOrCreateVisualRegion(regionId, region)
        if (player !in viewers) {
            outline.spawn(player)
            viewers.add(player)
        }
    }
    
    fun hideRegion(player: Player, regionId: UUID) {
        val (outline, viewers) = regions[regionId] ?: return
        outline.despawn(player)
        viewers.remove(player)
        
        if (viewers.isEmpty())
            removeRegion(regionId)
    }
    
    fun removeRegion(regionId: UUID) {
        val (outline, viewers) = regions.remove(regionId) ?: return
        outline.despawn(viewers)
    }
    
    fun updateRegion(regionId: UUID, region: Region) {
        val (outline, viewers) = regions[regionId] ?: return
        outline.despawn(viewers)
        val newOutline = createOutline(regionId, region)
        newOutline.spawn(viewers)
        regions[regionId] = newOutline to viewers
    }
    
    private fun getOrCreateVisualRegion(regionId: UUID, region: Region): Pair<List<FakeItemDisplay>, MutableSet<Player>> {
        return regions.getOrPut(regionId) {
            val outline = createOutline(regionId, region)
            val viewers = Collections.newSetFromMap<Player>(WeakHashMap())
            outline to viewers
        }
    }
    
    private fun createOutline(regionId: UUID, region: Region): List<FakeItemDisplay> {
        val min = region.min
        val max = region.max
        val color = Color(regionId.hashCode()).rgb
        
        return getEdgeDisplays(min, max, color)
    }
    
    private fun getEdgeDisplays(min: Location, max: Location, color: Int): List<FakeItemDisplay> {
        val (world, minX, minY, minZ) = min
        val (_, maxX, maxY, maxZ) = max
        
        // linearly weighted line width between MIN_LINE_WIDTH and MAX_LINE_WIDTH, depending on the diagonal length of the region
        val lineWidth = (min.distance(max) / DIAGONAL_THRESHOLD).coerceIn(0.0, 1.0) * (MAX_LINE_WIDTH - MIN_LINE_WIDTH) + MIN_LINE_WIDTH
        
        fun createLine(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) =
            createLine(Location(world, x1, y1, z1), Location(world, x2, y2, z2), lineWidth, color)
        
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
    
    private fun createLine(from: Location, to: Location, lineWidth: Double, color: Int): FakeItemDisplay {
        val center = from.clone().add(to).multiply(0.5)
        
        return FakeItemDisplay(center, false) { _, data ->
            data.itemDisplay = ItemDisplayContext.HEAD
            data.itemStack = DefaultBlockOverlays.TRANSPARENT_BLOCK.model.clientsideProvider.get().nmsCopy
            data.scale = Vector3f(
                (to.x - from.x + lineWidth).toFloat(),
                (to.y - from.y + lineWidth).toFloat(),
                (to.z - from.z + lineWidth).toFloat(),
            )
            data.isGlowing = true
            data.glowColor = color
        }
    }
    
}
