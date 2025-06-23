package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.World
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.xenondevs.nova.util.toLocation
import xyz.xenondevs.nova.util.toVector3f
import xyz.xenondevs.nova.world.fakeentity.impl.FakeInteraction

@Suppress("DuplicatedCode")
fun PhysicalHitbox(from: Location, to: Location): PhysicalHitbox {
    require(from.world != null && from.world == to.world) { "from and to must be in the same world" }
    require(from.x < to.x) { "from.x must be smaller than to.x" }
    require(from.y < to.y) { "from.y must be smaller than to.y" }
    require(from.z < to.z) { "from.z must be smaller than to.z" }
    
    val fromVec = from.toVector3f()
    val toVec = to.toVector3f()
    
    val width = toVec.x - fromVec.x
    require(width == toVec.z - fromVec.z) { "The hitbox base area must be a square" }
    val height = toVec.y - fromVec.y
    val baseCenter = Vector3f(fromVec.x + width / 2f, fromVec.y, fromVec.z + width / 2f)
    val center = Vector3f(fromVec.x + width / 2f, fromVec.y + height / 2f, fromVec.z + width / 2f)
    
    return PhysicalHitbox(
        from.world!!,
        baseCenter, center,
        fromVec, toVec,
        width, width, height
    )
}

class PhysicalHitbox internal constructor(
    world: World,
    baseCenter: Vector3fc, center: Vector3fc,
    from: Vector3fc, to: Vector3fc,
    xWidth: Float, zWidth: Float, height: Float
) : Hitbox<ClickHandler, ClickAtLocationHandler>(world, baseCenter, center, from, to, xWidth, zWidth, height) {
    
    private val centerLocation = baseCenter.toLocation(world)
    
    constructor(baseCenter: Location, width: Double, height: Double) : this(
        baseCenter.world!!,
        Vector3f(baseCenter.x.toFloat(), baseCenter.y.toFloat(), baseCenter.z.toFloat()),
        Vector3f(baseCenter.x.toFloat(), (baseCenter.y + height / 2).toFloat(), baseCenter.z.toFloat()),
        Vector3f((baseCenter.x - width / 2).toFloat(), baseCenter.y.toFloat(), (baseCenter.z - width / 2).toFloat()),
        Vector3f((baseCenter.x + width / 2).toFloat(), (baseCenter.y + height).toFloat(), (baseCenter.z + width / 2).toFloat()),
        width.toFloat(),
        width.toFloat(),
        height.toFloat()
    )
    
    internal fun createInteractionEntity(): FakeInteraction =
        FakeInteraction(centerLocation) { _, data ->
            data.width = xWidth // xWidth == zWidth in PhysicalHitbox
            data.height = height
        }
    
}