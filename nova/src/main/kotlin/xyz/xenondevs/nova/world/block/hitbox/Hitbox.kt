@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.abs

internal typealias ClickHandler = (Player) -> Unit
internal typealias ClickAtLocationHandler = (Player, Vector3f) -> Unit

private const val EPSILON = 0.0001f

abstract class Hitbox<L, R> internal constructor(
    val world: World,
    val baseCenter: Vector3fc,
    val center: Vector3fc,
    val from: Vector3fc,
    val to: Vector3fc,
    protected var xWidth: Float,
    protected var zWidth: Float,
    protected var height: Float
) {
    
    private val _leftClickHandlers = ArrayList<L>(1)
    val leftClickHandlers: List<L>
        get() = _leftClickHandlers
    private val _rightClickHandlers = ArrayList<R>(1)
    val rightClickHandlers: List<R>
        get() = _rightClickHandlers
    
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
        _leftClickHandlers += handler
    }
    
    /**
     * Adds a handler to this [Hitbox] that is called when a player right-clicks the hitbox.
     */
    fun addRightClickHandler(handler: R) {
        _rightClickHandlers += handler
    }
    
    /**
     * Determines whether a given [location] is inside this [Hitbox].
     */
    operator fun contains(location: Location): Boolean {
        return location.world == world
            && location.x in from.x()..to.x()
            && location.y in from.y()..to.y()
            && location.z in from.z()..to.z()
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