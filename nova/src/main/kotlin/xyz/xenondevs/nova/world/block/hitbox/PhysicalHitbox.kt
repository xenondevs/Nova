package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.World
import org.joml.Vector3f
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
    val center = Vector3f(fromVec.x + width / 2f, fromVec.y, fromVec.z + width / 2f)
    
    return PhysicalHitbox(
        from.world!!,
        center,
        fromVec, toVec,
        width, width, height
    )
}

class PhysicalHitbox internal constructor(
    world: World,
    center: Vector3f,
    from: Vector3f, to: Vector3f,
    xWidth: Float, zWidth: Float, height: Float
) : Hitbox<ClickHandler, ClickAtLocationHandler>(world, center, from, to, xWidth, zWidth, height) {
    
    private val centerLocation = center.toLocation(world)
    
    constructor(center: Location, width: Double, height: Double) : this(
        center.world!!,
        Vector3f(center.x.toFloat(), center.y.toFloat(), center.z.toFloat()),
        Vector3f((center.x - width / 2).toFloat(), center.y.toFloat(), (center.z - width / 2).toFloat()),
        Vector3f((center.x + width / 2).toFloat(), (center.y + height).toFloat(), (center.z + width / 2).toFloat()),
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