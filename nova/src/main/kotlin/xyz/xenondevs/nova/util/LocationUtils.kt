package xyz.xenondevs.nova.util

import com.google.common.base.Preconditions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.phys.Vec3
import org.bukkit.Axis
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockFace.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.joml.Vector3d
import org.joml.Vector3f
import xyz.xenondevs.nmsutils.particle.ParticleBuilder
import xyz.xenondevs.nmsutils.particle.color
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.item.isTraversable
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

val CUBE_FACES = listOf(NORTH, EAST, SOUTH, WEST, UP, DOWN)
val HORIZONTAL_FACES = listOf(NORTH, EAST, SOUTH, WEST)
val VERTICAL_FACES = listOf(UP, DOWN)

//<editor-fold desc="location creation / modification", defaultstate="collapsed">
val Location.blockLocation: Location
    get() = Location(world, blockX.toDouble(), blockY.toDouble(), blockZ.toDouble())

fun Location(world: World?, x: Int, y: Int, z: Int): Location =
    Location(world, x.toDouble(), y.toDouble(), z.toDouble())

fun Location.removeOrientation() {
    yaw = 0f
    pitch = 0f
}

fun Location.center(): Location =
    add(0.5, 0.0, 0.5)

fun Location.advance(blockFace: BlockFace, stepSize: Double = 1.0) =
    add(
        blockFace.modX.toDouble() * stepSize,
        blockFace.modY.toDouble() * stepSize,
        blockFace.modZ.toDouble() * stepSize
    )

fun Location.advance(axis: Axis, stepSize: Double = 1.0) = when (axis) {
    Axis.X -> add(stepSize, 0.0, 0.0)
    Axis.Y -> add(0.0, stepSize, 0.0)
    Axis.Z -> add(0.0, 0.0, stepSize)
}

fun Location.setCoordinate(axis: Axis, coordinate: Double) {
    when (axis) {
        Axis.X -> x = coordinate
        Axis.Y -> y = coordinate
        Axis.Z -> z = coordinate
    }
}

fun Location.getCoordinate(axis: Axis): Double {
    return when (axis) {
        Axis.X -> x
        Axis.Y -> y
        Axis.Z -> z
    }
}
//</editor-fold>

//<editor-fold desc="location components", defaultstate="collapsed">
operator fun Location.component1() = world
operator fun Location.component2() = x
operator fun Location.component3() = y
operator fun Location.component4() = z
operator fun Location.component5() = yaw
operator fun Location.component6() = pitch
//</editor-fold>

//<editor-fold desc="location info", defaultstate="collapsed">
fun Location.positionEquals(other: Location): Boolean =
    world == other.world
        && x == other.x
        && y == other.y
        && z == other.z

fun Location.isBlockLocation(): Boolean =
    x.toInt() - x == 0.0 && y.toInt() - y == 0.0 && z.toInt() - z == 0.0

fun Location.isBetween(min: Location, max: Location): Boolean =
    x in min.x.rangeTo(max.x)
        && y in min.y.rangeTo(max.y)
        && z in min.z.rangeTo(max.z)

fun Location.isBetweenXZ(min: Location, max: Location): Boolean =
    x in min.x.rangeTo(max.x)
        && z in min.z.rangeTo(max.z)

/**
 * Checks if a location is inside the world borders and in the allowed building height.
 */
fun Location.isInsideWorldRestrictions(): Boolean {
    val world = world!!
    return world.worldBorder.isInside(this) && blockY in world.minHeight until world.maxHeight
}
//</editor-fold>

//<editor-fold desc="direction / vector", defaultstate="collapsed">
fun Vector.calculateYawPitch(): FloatArray {
    // Minecraft's coordinate system is weird
    val x = this.z
    val y = -this.x
    val z = this.y
    
    val yaw = atan2(y, x)
    val h = y / sin(yaw)
    val pitch = atan2(z, h)
    
    return floatArrayOf(Math.toDegrees(yaw).toFloat(), -Math.toDegrees(pitch).toFloat())
}

fun Vector.calculateYaw(): Float {
    val x = this.z
    val y = -this.x
    
    val yaw = atan2(y, x)
    return Math.toDegrees(yaw).toFloat()
}

fun Vector(yaw: Float, pitch: Float): Vector {
    val pitchRadians = Math.toRadians(-pitch.toDouble())
    val yawRadians = Math.toRadians(yaw.toDouble())
    val xy = cos(pitchRadians)
    val x = cos(yawRadians) * xy
    val y = sin(yawRadians) * xy
    val z = sin(pitchRadians)
    return Vector(-y, z, x)
}

fun Location.toVector3d(): Vector3d =
    Vector3d(x, y, z)

fun Location.toVector3f(): Vector3f =
    Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

fun Location.toVec3(): Vec3 =
    Vec3(x, y, z)

fun Vector3d.toLocation(world: World? = null): Location =
    Location(world, x, y, z)

fun Vector3d.toVec3(): Vec3 =
    Vec3(x, y, z)

fun Vector3f.toLocation(world: World? = null): Location =
    Location(world, x.toDouble(), y.toDouble(), z.toDouble())

fun Vector3f.toVec3(): Vec3 =
    Vec3(x.toDouble(), y.toDouble(), z.toDouble())

fun Vector.toVector3d(): Vector3d =
    Vector3d(x, y, z)

fun Vector.toVector3f(): Vector3f =
    Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

fun Vector.toVec3(): Vec3 =
    Vec3(x, y, z)
//</editor-fold>

//<editor-fold desc="surrounding blocks / entities / etc.", defaultstate="collapsed">
fun Location.getNeighboringTileEntities(additionalHitboxes: Boolean): Map<BlockFace, TileEntity> {
    return getNeighboringTileEntitiesOfType(additionalHitboxes)
}

internal inline fun <reified T> Location.getNeighboringTileEntitiesOfType(additionalHitboxes: Boolean): Map<BlockFace, T> {
    val tileEntities = HashMap<BlockFace, T>()
    CUBE_FACES.forEach {
        val location = blockLocation.advance(it)
        val tileEntity = TileEntityManager.getTileEntity(location, additionalHitboxes)
            ?: VanillaTileEntityManager.getTileEntityAt(location)
        if (tileEntity != null && tileEntity is T) tileEntities[it] = tileEntity as T
    }
    
    return tileEntities
}

fun Chunk.getSurroundingChunks(range: Int, includeCurrent: Boolean, ignoreUnloaded: Boolean = false): List<Chunk> {
    val chunks = ArrayList<Chunk>()
    val world = world
    for (chunkX in (x - range)..(x + range)) {
        for (chunkZ in (z - range)..(z + range)) {
            if (ignoreUnloaded && !world.isChunkLoaded(chunkX, chunkZ)) continue
            
            val chunk = world.getChunkAt(chunkX, chunkZ)
            if (chunk != this || includeCurrent)
                chunks += world.getChunkAt(chunkX, chunkZ)
        }
    }
    
    return chunks
}

fun Location.getPlayersNearby(maxDistance: Double, vararg excluded: Player): Sequence<Player> {
    val maxDistanceSquared = maxDistance * maxDistance
    
    return world!!.players
        .asSequence()
        .filter { it !in excluded }
        .filter { distanceSquared(it.location) <= maxDistanceSquared }
}

fun Iterable<Player>.filterInRange(location: Location, maxDistance: Double): List<Player> {
    val maxDistanceSquared = maxDistance * maxDistance
    return filter {
        val otherLocation = it.location
        location.world == otherLocation.world && location.distanceSquared(otherLocation) <= maxDistanceSquared
    }
}
//</editor-fold>

//<editor-fold desc="stepping", defaultstate="collapsed">
inline fun Location.castRay(stepSize: Double, maxDistance: Double, run: (Location) -> Boolean) {
    val vector = direction.multiply(stepSize)
    val location = clone()
    var distance = 0.0
    while (run(location) && distance <= maxDistance) {
        location.add(vector)
        distance += stepSize
    }
}

fun Location.getTargetLocation(stepSize: Double, maxDistance: Double): Location {
    var location = this
    
    castRay(stepSize, maxDistance) { rayLocation ->
        val block = rayLocation.block
        if (block.type.isTraversable() && !block.boundingBox.contains(rayLocation.x, rayLocation.y, rayLocation.z)) {
            location = rayLocation.clone()
            return@castRay true
        } else {
            return@castRay false
        }
    }
    
    return location
}

fun Location.untilHeightLimit(includeThis: Boolean, run: (Location) -> Boolean) {
    val heightLimit = world!!.maxHeight
    val location = clone().apply { if (!includeThis) add(0.0, 1.0, 0.0) }
    while (location.y < heightLimit) {
        if (!run(location)) break
        
        location.add(0.0, 1.0, 0.0)
    }
}

fun Location.getNextBlockBelow(countSelf: Boolean, requiresSolid: Boolean): Location? {
    val location = clone()
    if (!countSelf) location.y -= 1
    while (location.y >= (world?.minHeight ?: -64)) {
        val type = location.block.type
        if (!type.isAir && (!requiresSolid || type.isSolid)) return location
        location.y -= 1
    }
    
    return null
}
//</editor-fold>

//<editor-fold desc="shapes", defaultstate="collapsed">
fun Location.getStraightLine(axis: Axis, to: Int): List<Location> {
    val min = min(getCoordinate(axis).toInt(), to)
    val max = max(getCoordinate(axis).toInt(), to)
    
    return (min..max).map { coordinate -> clone().apply { setCoordinate(axis, coordinate.toDouble()) } }
}

fun Location.getRectangle(to: Location, omitCorners: Boolean): Map<Axis, List<Location>> {
    val rectangle = HashMap<Axis, List<Location>>()
    val listX = ArrayList<Location>().also { rectangle[Axis.X] = it }
    val listZ = ArrayList<Location>().also { rectangle[Axis.Z] = it }
    
    val modifier = if (omitCorners) 1 else 0
    
    val minX = min(blockX, to.blockX)
    val minZ = min(blockZ, to.blockZ)
    val maxX = max(blockX, to.blockX)
    val maxZ = max(blockZ, to.blockZ)
    
    clone()
        .apply { x = (minX + modifier).toDouble() }
        .getStraightLine(Axis.X, (maxX - modifier))
        .forEach { location ->
            listX += location.clone().apply { z = minZ.toDouble() }
            listX += location.clone().apply { z = maxZ.toDouble() }
        }
    
    clone()
        .apply { z = (minZ + modifier).toDouble() }
        .getStraightLine(Axis.Z, (maxZ - modifier))
        .forEach { location ->
            listZ += location.clone().apply { x = minX.toDouble() }
            listZ += location.clone().apply { x = maxX.toDouble() }
        }
    
    return rectangle
}

inline fun Location.fullCuboidTo(to: Location, run: (Location) -> Boolean) {
    Preconditions.checkArgument(world != null && to.world == world)
    
    val (min, max) = LocationUtils.sort(this, to)
    for (x in min.blockX..max.blockX) {
        for (y in min.blockY..max.blockY) {
            for (z in min.blockZ..max.blockZ) {
                val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                if (!run(location)) return
            }
        }
    }
}

fun Location.getBoxOutline(other: Location, correct: Boolean, stepSize: Double = 0.5): List<Location> {
    val locations = ArrayList<Location>()
    
    val minX = min(x, other.x)
    val minY = min(y, other.y)
    val minZ = min(z, other.z)
    val maxX = max(x, other.x) + if (correct) 1 else 0
    val maxY = max(y, other.y) + if (correct) 1 else 0
    val maxZ = max(z, other.z) + if (correct) 1 else 0
    
    // lines in x direction
    var x = minX
    while (x < maxX) {
        x += stepSize
        locations.add(Location(world, x, minY, minZ))
        locations.add(Location(world, x, maxY, minZ))
        locations.add(Location(world, x, minY, maxZ))
        locations.add(Location(world, x, maxY, maxZ))
    }
    
    // lines in z direction
    var z = minZ
    while (z < maxZ) {
        z += stepSize
        locations.add(Location(world, minX, minY, z))
        locations.add(Location(world, maxX, minY, z))
        locations.add(Location(world, minX, maxY, z))
        locations.add(Location(world, maxX, maxY, z))
    }
    
    // lines in y direction
    var y = minY
    while (y < maxY) {
        y += stepSize
        locations.add(Location(world, minX, y, minZ))
        locations.add(Location(world, maxX, y, minZ))
        locations.add(Location(world, minX, y, maxZ))
        locations.add(Location(world, maxX, y, maxZ))
    }
    
    return locations
}

fun Location.getFullCuboid(other: Location): List<Location> {
    Preconditions.checkArgument(world != null && other.world == world)
    
    val list = ArrayList<Location>()
    val (min, max) = LocationUtils.sort(this, other)
    for (x in min.blockX..max.blockX) {
        for (y in min.blockY..max.blockY) {
            for (z in min.blockZ..max.blockZ) {
                list += Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            }
        }
    }
    
    return list
}
//</editor-fold>

//<editor-fold desc="items", defaultstate="collapsed">
fun Location.dropItems(items: Iterable<ItemStack>) {
    val world = world!!
    items.forEach { world.dropItemNaturally(this, it) }
}

fun Location.dropItem(item: ItemStack) {
    world!!.dropItemNaturally(this, item)
}

fun World.dropItemsNaturally(location: Location, items: Iterable<ItemStack>) {
    items.forEach { dropItemNaturally(location, it) }
}
//</editor-fold>

//<editor-fold desc="particles">
fun Location.createColoredParticle(color: Color): ClientboundLevelParticlesPacket =
    ParticleBuilder(ParticleTypes.DUST, this).color(color).build()
//</editor-fold>

object LocationUtils {
    
    fun getTopBlockBetween(
        world: World,
        x: Int, z: Int,
        maxY: Int, minY: Int
    ): Location? {
        val location = Location(world, x, 0, z)
        for (y in maxY downTo minY) {
            location.y = y.toDouble()
            if (!location.block.type.isTraversable())
                return location
        }
        
        return null
    }
    
    fun getStraightLine(base: Location, axis: Axis, range: IntRange) =
        range.map { base.clone().apply { setCoordinate(axis, it.toDouble()) } }
    
    fun sort(first: Location, second: Location): Pair<Location, Location> {
        Preconditions.checkArgument(first.world == second.world)
        
        val minX = min(first.x, second.x)
        val minY = min(first.y, second.y)
        val minZ = min(first.z, second.z)
        val maxX = max(first.x, second.x)
        val maxY = max(first.y, second.y)
        val maxZ = max(first.z, second.z)
        
        return Location(first.world, minX, minY, minZ) to Location(second.world, maxX, maxY, maxZ)
    }
    
}