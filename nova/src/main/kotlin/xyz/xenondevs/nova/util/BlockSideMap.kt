package xyz.xenondevs.nova.util

import org.bukkit.block.BlockFace

/**
 * Runs [run] for each non-null value in this [BlockSideMap].
 */
inline fun <V : Any> BlockSideMap<V?>.forEachNonNull(run: (value: V) -> Unit) =
    forEach { if (it != null) run(it) }

/**
 * Runs [run] for each non-null value in this [BlockSideMap], providing both the [BlockSide] and value.
 */
inline fun <V : Any> BlockSideMap<V?>.forEachNonNull(run: (side: BlockSide, value: V) -> Unit) =
    forEach { side, value -> if (value != null) run(side, value) }

/**
 * Creates a [BlockSideMap] by calling [init] for each [BlockSide].
 */
inline fun <V> BlockSideMap(init: (side: BlockSide) -> V): BlockSideMap<V> =
    BlockSideMap(
        front = init(BlockSide.FRONT),
        left = init(BlockSide.LEFT),
        back = init(BlockSide.BACK),
        right = init(BlockSide.RIGHT),
        top = init(BlockSide.TOP),
        bottom = init(BlockSide.BOTTOM)
    )

/**
 * An immutable map from each [BlockSide] to a value of type [V].
 * Keys cannot be absent, use `null` to model absence instead.
 */
data class BlockSideMap<out V>(
    val front: V,
    val left: V,
    val back: V,
    val right: V,
    val top: V,
    val bottom: V
) {
    
    /**
     * A list of all non-null values in this map.
     */
    val values: Collection<V & Any> = run {
        val list = ArrayList<V & Any>(6)
        if (front != null) list.add(front)
        if (left != null) list.add(left)
        if (back != null) list.add(back)
        if (right != null) list.add(right)
        if (top != null) list.add(top)
        if (bottom != null) list.add(bottom)
        list
    }
    
    constructor(all: V) : this(all, all, all, all, all, all)
    
    /**
     * Gets the value for the given [side].
     */
    operator fun get(side: BlockSide) = when (side) {
        BlockSide.FRONT -> front
        BlockSide.LEFT -> left
        BlockSide.BACK -> back
        BlockSide.RIGHT -> right
        BlockSide.TOP -> top
        BlockSide.BOTTOM -> bottom
    }
    
    /**
     * Returns a copy of this map with the value for [side] replaced by [value].
     */
    fun with(side: BlockSide, value: @UnsafeVariance V) = when (side) {
        BlockSide.FRONT -> copy(front = value)
        BlockSide.LEFT -> copy(left = value)
        BlockSide.BACK -> copy(back = value)
        BlockSide.RIGHT -> copy(right = value)
        BlockSide.TOP -> copy(top = value)
        BlockSide.BOTTOM -> copy(bottom = value)
    }
    
    /**
     * Returns a new [BlockSideMap] with each value transformed by [map].
     */
    inline fun <R> map(map: (value: V) -> R): BlockSideMap<R> =
        BlockSideMap(
            front = map(front),
            left = map(left),
            back = map(back),
            right = map(right),
            top = map(top),
            bottom = map(bottom)
        )
    
    /**
     * Returns a new [BlockSideMap] with each value transformed by [map], which also receives the [BlockSide].
     */
    inline fun <R> map(map: (side: BlockSide, value: V) -> R): BlockSideMap<R> =
        BlockSideMap(
            front = map(BlockSide.FRONT, front),
            left = map(BlockSide.LEFT, left),
            back = map(BlockSide.BACK, back),
            right = map(BlockSide.RIGHT, right),
            top = map(BlockSide.TOP, top),
            bottom = map(BlockSide.BOTTOM, bottom)
        )
    
    /**
     * Returns a new [BlockSideMap] where values not matching [filter] are replaced with `null`.
     */
    inline fun filter(filter: (value: V & Any) -> Boolean): BlockSideMap<V?> =
        BlockSideMap(
            front = if (front != null && filter(front)) front else null,
            left = if (left != null && filter(left)) left else null,
            back = if (back != null && filter(back)) back else null,
            right = if (right != null && filter(right)) right else null,
            top = if (top != null && filter(top)) top else null,
            bottom = if (bottom != null && filter(bottom)) bottom else null
        )
    
    /**
     * Returns a new [BlockSideMap] where values not matching [filter] are replaced with `null`.
     * The filter also receives the [BlockSide].
     */
    inline fun filter(filter: (side: BlockSide, value: V & Any) -> Boolean): BlockSideMap<V?> =
        BlockSideMap(
            front = if (front != null && filter(BlockSide.FRONT, front)) front else null,
            left = if (left != null && filter(BlockSide.LEFT, left)) left else null,
            back = if (back != null && filter(BlockSide.BACK, back)) back else null,
            right = if (right != null && filter(BlockSide.RIGHT, right)) right else null,
            top = if (top != null && filter(BlockSide.TOP, top)) top else null,
            bottom = if (bottom != null && filter(BlockSide.BOTTOM, bottom)) bottom else null
        )
    
    /**
     * Runs [run] for each value in this map.
     */
    inline fun forEach(run: (value: V) -> Unit) {
        run(front)
        run(left)
        run(back)
        run(right)
        run(top)
        run(bottom)
    }
    
    /**
     * Runs [run] for each side-value pair in this map.
     */
    inline fun forEach(run: (side: BlockSide, value: V) -> Unit) {
        run(BlockSide.FRONT, front)
        run(BlockSide.LEFT, left)
        run(BlockSide.BACK, back)
        run(BlockSide.RIGHT, right)
        run(BlockSide.TOP, top)
        run(BlockSide.BOTTOM, bottom)
    }
    
    /**
     * Converts this [BlockSideMap] to a [CubeFaceMap] given the [front] direction.
     */
    fun toCubeFaceMap(front: BlockFace): CubeFaceMap<V> =
        CubeFaceMap(
            north = this[BlockSide.of(front, BlockFace.NORTH)],
            east = this[BlockSide.of(front, BlockFace.EAST)],
            south = this[BlockSide.of(front, BlockFace.SOUTH)],
            west = this[BlockSide.of(front, BlockFace.WEST)],
            up = top,
            down = bottom
        )
    
    /**
     * Returns a [BlockSideSet] containing all sides for which [map] returns `true`.
     */
    inline fun mapToBlockSideSet(map: (value: V) -> Boolean): BlockSideSet {
        var data = 0
        if (map(front))
            data = data or 0b000001
        if (map(left))
            data = data or 0b000010
        if (map(back))
            data = data or 0b000100
        if (map(right))
            data = data or 0b001000
        if (map(top))
            data = data or 0b010000
        if (map(bottom))
            data = data or 0b100000
        return BlockSideSet(data.toByte())
    }
    
    companion object {
        
        /** A [BlockSideMap] with all sides mapped to `null`. */
        val NULL = BlockSideMap(null)
        
    }
    
}
