package xyz.xenondevs.nova.world.block

import kotlinx.serialization.Serializable

private const val MIN_ENTITY_COLLIDER_SIZE = 1.0 / 16.0
private const val MAX_ENTITY_COLLIDER_SIZE = 16.0

/**
 * A cube-shaped hard collider for an entity-backed block model.
 *
 * Coordinates are relative to the block's position.
 * The cube may be positioned outside the block.
 */
@Serializable
data class ColliderCube(
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
     * The length of every edge.
     * Must be between 1/16 and 16 blocks.
     */
    val size: Double
) {
    
    /**
     * The maximum x-coordinate relative to the block's position.
     */
    val maxX = minX + size
    
    /**
     * The maximum y-coordinate relative to the block's position.
     */
    val maxY = minY + size
    
    /**
     * The maximum z-coordinate relative to the block's position.
     */
    val maxZ = minZ + size
    
    /**
     * The x-coordinate of the cube's center relative to the block's position.
     */
    val centerX = minX + size / 2.0
    
    /**
     * The y-coordinate of the cube's center relative to the block's position.
     */
    val centerY = minY + size / 2.0
    
    /**
     * The z-coordinate of the cube's center relative to the block's position.
     */
    val centerZ = minZ + size / 2.0
    
    init {
        require(minX.isFinite()) { "minX must be finite" }
        require(minY.isFinite()) { "minY must be finite" }
        require(minZ.isFinite()) { "minZ must be finite" }
        require(size in MIN_ENTITY_COLLIDER_SIZE..MAX_ENTITY_COLLIDER_SIZE) { "size must be in $MIN_ENTITY_COLLIDER_SIZE..$MAX_ENTITY_COLLIDER_SIZE" }
    }
    
}
