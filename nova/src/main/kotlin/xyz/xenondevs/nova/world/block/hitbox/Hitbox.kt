@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.joml.Vector3f
import kotlin.math.abs

internal typealias ClickHandler = (Player) -> Unit
internal typealias ClickAtLocationHandler = (Player, EquipmentSlot, Vector3f) -> Unit

private const val EPSILON = 0.0001f

abstract class Hitbox<L, R> internal constructor(
    internal val world: World,
    internal val center: Vector3f,
    internal val from: Vector3f,
    internal val to: Vector3f,
    protected var xWidth: Float,
    protected var zWidth: Float,
    protected var height: Float
) {
    
    internal val leftClickHandlers = ArrayList<L>(1)
    internal val rightClickHandlers = ArrayList<R>(1)
    
    /**
     * Adds the [Hitbox] to the world.
     */
    fun register() {
        HitboxManager.registerHitbox(this)
    }
    
    /**
     * Removes the [Hitbox] from the world.
     */
    fun remove() {
        HitboxManager.removeHitbox(this)
    }
    
    /**
     * Adds a handler to this [Hitbox] that is called when a player left-clicks the hitbox
     */
    fun addLeftClickHandler(handler: L) {
        leftClickHandlers += handler
    }
    
    /**
     * Adds a handler to this [Hitbox] that is called when a player right-clicks the hitbox.
     */
    fun addRightClickHandler(handler: R) {
        rightClickHandlers += handler
    }
    
    /**
     * Determines whether a given [location] is inside this [Hitbox].
     */
    operator fun contains(location: Location): Boolean {
        return location.world == world
            && location.x in from.x..to.x
            && location.y in from.y..to.y
            && location.z in from.z..to.z
    }
    
    /**
     * Determines the clicked [BlockFace] of the hitbox based on the relative [location] provided by the
     * interact handler. Returns null if the [location] is inside the hitbox.
     */
    fun determineBlockFace(location: Vector3f): BlockFace? =
        when {
            location.y == 0f -> BlockFace.DOWN
            abs(location.y - height) < EPSILON -> BlockFace.UP
            abs(location.x + xWidth / 2) < EPSILON -> BlockFace.WEST
            abs(location.x - xWidth / 2) < EPSILON -> BlockFace.EAST
            abs(location.z + zWidth / 2) < EPSILON -> BlockFace.NORTH
            abs(location.z - zWidth / 2) < EPSILON -> BlockFace.SOUTH
            else -> null // potentially inside the hitbox
        }
    
}