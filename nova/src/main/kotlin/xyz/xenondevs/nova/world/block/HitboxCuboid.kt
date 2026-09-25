package xyz.xenondevs.nova.world.block

import kotlinx.serialization.Serializable

/**
 * An interaction hitbox for an entity-backed block model.
 * Coordinates are relative to the block's position.
 * The hitbox has a square base and an independent height.
 */
@Serializable
data class HitboxCuboid(
    /**
     * The minimum x-coordinate relative to the block's position.
     */
    val minX: Double,
    /**
     * The minimum y-coordinate relative to the block's position.
     */
    val minY: Double,
    /**
     * The minimum z-coordinate relative to the block's position.
     */
    val minZ: Double,
    /**
     * The length of the hitbox along the x and z axes.
     * Must be positive and representable as a float.
     */
    val width: Double,
    /**
     * The length of the hitbox along the y axis.
     * Must be positive and representable as a float.
     */
    val height: Double
) {
    
    /**
     * The maximum x-coordinate relative to the block's position.
     */
    val maxX = minX + width
    
    /**
     * The maximum y-coordinate relative to the block's position.
     */
    val maxY = minY + height
    
    /**
     * The maximum z-coordinate relative to the block's position.
     */
    val maxZ = minZ + width
    
    /**
     * The x-coordinate of the hitbox's center relative to the block's position.
     */
    val centerX = minX + width / 2.0
    
    /**
     * The y-coordinate of the hitbox's center relative to the block's position.
     */
    val centerY = minY + height / 2.0
    
    /**
     * The z-coordinate of the hitbox's center relative to the block's position.
     */
    val centerZ = minZ + width / 2.0
    
    init {
        require(minX.isFinite() && minY.isFinite() && minZ.isFinite()) { "Hitbox coordinates must be finite" }
        require(width > 0.0 && width.toFloat().isFinite()) { "width must be positive and fit in a float" }
        require(height > 0.0 && height.toFloat().isFinite()) { "height must be positive and fit in a float" }
    }
    
    internal companion object {
        fun fromCollider(cube: ColliderCube) = HitboxCuboid(cube.minX, cube.minY, cube.minZ, cube.size, cube.size)
    }
    
}
