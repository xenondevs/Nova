package xyz.xenondevs.nova.world.region

import net.minecraft.world.item.ItemDisplayContext
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Vector3f
import xyz.xenondevs.nova.material.CoreBlockOverlay
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

private const val MIN_SCALE = 0.05f

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
        
        return listOf(
            // minX -> maxX
            createLine(Location(world, minX, minY, minZ), Location(world, maxX, minY, minZ), color),
            createLine(Location(world, minX, minY, maxZ), Location(world, maxX, minY, maxZ), color),
            createLine(Location(world, minX, maxY, minZ), Location(world, maxX, maxY, minZ), color),
            createLine(Location(world, minX, maxY, maxZ), Location(world, maxX, maxY, maxZ), color),
            // minY -> maxY
            createLine(Location(world, minX, minY, minZ), Location(world, minX, maxY, minZ), color),
            createLine(Location(world, minX, minY, maxZ), Location(world, minX, maxY, maxZ), color),
            createLine(Location(world, maxX, minY, minZ), Location(world, maxX, maxY, minZ), color),
            createLine(Location(world, maxX, minY, maxZ), Location(world, maxX, maxY, maxZ), color),
            // minZ -> maxZ
            createLine(Location(world, minX, minY, minZ), Location(world, minX, minY, maxZ), color),
            createLine(Location(world, minX, maxY, minZ), Location(world, minX, maxY, maxZ), color),
            createLine(Location(world, maxX, minY, minZ), Location(world, maxX, minY, maxZ), color),
            createLine(Location(world, maxX, maxY, minZ), Location(world, maxX, maxY, maxZ), color),
        )
    }
    
    private fun createLine(from: Location, to: Location, color: Int): FakeItemDisplay {
        val center = from.clone().add(to).multiply(0.5)
        
        return FakeItemDisplay(center, false) { _, data ->
            data.itemDisplay = ItemDisplayContext.HEAD
            data.itemStack = CoreBlockOverlay.TRANSPARENT_BLOCK.clientsideProvider.get().nmsCopy
            data.scale = Vector3f(
                (to.x - from.x + MIN_SCALE).toFloat(),
                (to.y - from.y + MIN_SCALE).toFloat(),
                (to.z - from.z + MIN_SCALE).toFloat(),
            )
            data.isGlowing = true
            data.glowColor = color
        }
    }
    
}
