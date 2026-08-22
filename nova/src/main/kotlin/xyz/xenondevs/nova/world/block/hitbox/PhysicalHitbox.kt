package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.joml.Intersectionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.packetentity.packetInteraction
import xyz.xenondevs.nova.util.toLocation
import xyz.xenondevs.nova.util.toVector3f
import xyz.xenondevs.nova.world.InteractionResult

/**
 * Creates a hitbox backed by a client-side interaction entity.
 *
 * Click handlers are invoked only when protection integrations allow the player to use the block
 * at the hit position.
 */
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

/**
 * A hitbox backed by a client-side interaction entity.
 *
 * Click handlers are invoked only when protection integrations allow the player to use the block
 * at the hit position.
 */
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
    
    internal fun createInteractionEntity() = packetInteraction {
        location by centerLocation
        metadata {
            width by xWidth // xWidth == zWidth in PhysicalHitbox
            height by this@PhysicalHitbox.height
        }
        onAttack {
            val hitLocation = findHitLocation(player)
                ?: return@onAttack
            if (!canUseBlock(player, hitLocation))
                return@onAttack
            
            leftClickHandlers.forEach { it(player) }
        }
        onInteract {
            val hitLocation = Vector3f(
                interactLocation.x().toFloat(),
                interactLocation.y().toFloat(),
                interactLocation.z().toFloat()
            )
            if (!canUseBlock(player, hitLocation))
                return@onInteract InteractionResult.Pass
            
            rightClickHandlers.forEach { it(player, hitLocation) }
            InteractionResult.Success()
        }
    }
    
    private fun findHitLocation(player: Player): Vector3f? {
        val eye = player.eyeLocation
        val direction = eye.direction
        val origin = Vector3f(eye.x.toFloat(), eye.y.toFloat(), eye.z.toFloat())
        val directionVector = Vector3f(direction.x.toFloat(), direction.y.toFloat(), direction.z.toFloat()).normalize()
        val distances = Vector2f()
        if (!Intersectionf.intersectRayAab(origin, directionVector, from, to, distances))
            return null
        
        val distance = if (distances.x >= 0f) distances.x else distances.y
        val interactionRange = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value.toFloat()
        if (distance !in 0f..interactionRange)
            return null
        
        return Vector3f(directionVector)
            .mul(distance)
            .add(origin)
            .sub(baseCenter)
    }
    
    private fun canUseBlock(player: Player, hitLocation: Vector3f): Boolean {
        val block = Vector3f(baseCenter)
            .add(hitLocation)
            .toLocation(world)
            .block
        return ProtectionManager.canUseBlock(player, player.inventory.itemInMainHand, block)
    }
    
}