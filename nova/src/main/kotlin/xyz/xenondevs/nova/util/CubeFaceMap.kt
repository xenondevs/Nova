package xyz.xenondevs.nova.util

import xyz.xenondevs.nova.world.*

import org.bukkit.block.BlockFace

/**
 * Runs [run] for each non-null value in this [CubeFaceMap].
 */
inline fun <V : Any> CubeFaceMap<V?>.forEachNonNull(run: (value: V) -> Unit) =
    forEach { if (it != null) run(it) }

/**
 * Runs [run] for each non-null value in this [CubeFaceMap], providing both the [BlockFace] and value.
 */
inline fun <V : Any> CubeFaceMap<V?>.forEachNonNull(run: (face: BlockFace, value: V) -> Unit) =
    forEach { face, value -> if (value != null) run(face, value) }

/**
 * Creates a [CubeFaceMap] by calling [init] for each cube [BlockFace].
 */
inline fun <V> CubeFaceMap(init: (face: BlockFace) -> V): CubeFaceMap<V> =
    CubeFaceMap(
        north = init(BlockFace.NORTH),
        east = init(BlockFace.EAST),
        south = init(BlockFace.SOUTH),
        west = init(BlockFace.WEST),
        up = init(BlockFace.UP),
        down = init(BlockFace.DOWN)
    )

/**
 * An immutable map from each [BlockFace] (of the cube faces [BlockFace.NORTH], [BlockFace.EAST],
 * [BlockFace.SOUTH], [BlockFace.WEST], [BlockFace.UP], [BlockFace.DOWN]) to a value of type [V].
 * Keys cannot be absent, use `null` to model absence instead.
 */
data class CubeFaceMap<out V>(
    val north: V,
    val east: V,
    val south: V,
    val west: V,
    val up: V,
    val down: V
) {
    
    /**
     * A list of all non-null values in this map.
     */
    val values: Collection<V & Any> = run {
        val list = ArrayList<V & Any>(6)
        if (north != null) list.add(north)
        if (east != null) list.add(east)
        if (south != null) list.add(south)
        if (west != null) list.add(west)
        if (up != null) list.add(up)
        if (down != null) list.add(down)
        list
    }
    
    constructor(all: V) : this(all, all, all, all, all, all)
    
    /**
     * Gets the value for the given [face].
     *
     * @throws IllegalArgumentException if [face] is not a cube face.
     */
    operator fun get(face: BlockFace) = when (face) {
        BlockFace.NORTH -> north
        BlockFace.EAST -> east
        BlockFace.SOUTH -> south
        BlockFace.WEST -> west
        BlockFace.UP -> up
        BlockFace.DOWN -> down
        else -> throw IllegalArgumentException("Not a cube face: $face")
    }
    
    /**
     * Returns a copy of this map with the value for [face] replaced by [value].
     *
     * @throws IllegalArgumentException if [face] is not a cube face.
     */
    fun with(face: BlockFace, value: @UnsafeVariance V) = when (face) {
        BlockFace.NORTH -> copy(north = value)
        BlockFace.EAST -> copy(east = value)
        BlockFace.SOUTH -> copy(south = value)
        BlockFace.WEST -> copy(west = value)
        BlockFace.UP -> copy(up = value)
        BlockFace.DOWN -> copy(down = value)
        else -> throw IllegalArgumentException("Not a cube face: $face")
    }
    
    /**
     * Returns a new [CubeFaceMap] with each value transformed by [map].
     */
    inline fun <R> map(map: (value: V) -> R): CubeFaceMap<R> =
        CubeFaceMap(
            north = map(north),
            east = map(east),
            south = map(south),
            west = map(west),
            up = map(up),
            down = map(down)
        )
    
    /**
     * Returns a new [CubeFaceMap] with each value transformed by [map], which also receives the [BlockFace].
     */
    inline fun <R> map(map: (face: BlockFace, value: V) -> R): CubeFaceMap<R> =
        CubeFaceMap(
            north = map(BlockFace.NORTH, north),
            east = map(BlockFace.EAST, east),
            south = map(BlockFace.SOUTH, south),
            west = map(BlockFace.WEST, west),
            up = map(BlockFace.UP, up),
            down = map(BlockFace.DOWN, down)
        )
    
    /**
     * Returns a new [CubeFaceMap] where values not matching [filter] are replaced with `null`.
     */
    inline fun filter(filter: (value: V & Any) -> Boolean): CubeFaceMap<V?> =
        CubeFaceMap(
            north = if (north != null && filter(north)) north else null,
            east = if (east != null && filter(east)) east else null,
            south = if (south != null && filter(south)) south else null,
            west = if (west != null && filter(west)) west else null,
            up = if (up != null && filter(up)) up else null,
            down = if (down != null && filter(down)) down else null
        )
    
    /**
     * Returns a new [CubeFaceMap] where values not matching [filter] are replaced with `null`.
     * The filter also receives the [BlockFace].
     */
    inline fun filter(filter: (face: BlockFace, value: V & Any) -> Boolean): CubeFaceMap<V?> =
        CubeFaceMap(
            north = if (north != null && filter(BlockFace.NORTH, north)) north else null,
            east = if (east != null && filter(BlockFace.EAST, east)) east else null,
            south = if (south != null && filter(BlockFace.SOUTH, south)) south else null,
            west = if (west != null && filter(BlockFace.WEST, west)) west else null,
            up = if (up != null && filter(BlockFace.UP, up)) up else null,
            down = if (down != null && filter(BlockFace.DOWN, down)) down else null
        )
    
    /**
     * Runs [run] for each value in this map.
     */
    inline fun forEach(run: (value: V) -> Unit) {
        run(north)
        run(east)
        run(south)
        run(west)
        run(up)
        run(down)
    }
    
    /**
     * Runs [run] for each face-value pair in this map.
     */
    inline fun forEach(run: (face: BlockFace, value: V) -> Unit) {
        run(BlockFace.NORTH, north)
        run(BlockFace.EAST, east)
        run(BlockFace.SOUTH, south)
        run(BlockFace.WEST, west)
        run(BlockFace.UP, up)
        run(BlockFace.DOWN, down)
    }
    
    /**
     * Converts this [CubeFaceMap] to a [BlockSideMap] given the [front] direction.
     */
    fun toBlockSideMap(front: BlockFace): BlockSideMap<V> =
        BlockSideMap(
            front = this[front],
            left = this[BlockSide.LEFT.getBlockFace(front)],
            back = this[BlockSide.BACK.getBlockFace(front)],
            right = this[BlockSide.RIGHT.getBlockFace(front)],
            top = up,
            bottom = down
        )
    
    /**
     * Returns a [CubeFaceSet] containing all faces for which [map] returns `true`.
     */
    inline fun mapToCubeFaceSet(map: (value: V) -> Boolean): CubeFaceSet {
        var data = 0
        if (map(north))
            data = data or 0b000001
        if (map(east))
            data = data or 0b000010
        if (map(south))
            data = data or 0b000100
        if (map(west))
            data = data or 0b001000
        if (map(up))
            data = data or 0b010000
        if (map(down))
            data = data or 0b100000
        return CubeFaceSet(data.toByte())
    }
    
    companion object {
        
        /** A [CubeFaceMap] with all faces mapped to `null`. */
        val NULL = CubeFaceMap(null)
        
    }
    
}
