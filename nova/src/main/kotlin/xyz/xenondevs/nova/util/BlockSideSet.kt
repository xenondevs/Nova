package xyz.xenondevs.nova.util

import xyz.xenondevs.nova.world.*

import org.bukkit.block.BlockFace

/**
 * Creates a [BlockSideSet] containing each [BlockSide] for which [init] returns `true`.
 */
inline fun BlockSideSet(init: (side: BlockSide) -> Boolean): BlockSideSet =
    BlockSideSet(
        front = init(BlockSide.FRONT),
        left = init(BlockSide.LEFT),
        back = init(BlockSide.BACK),
        right = init(BlockSide.RIGHT),
        top = init(BlockSide.TOP),
        bottom = init(BlockSide.BOTTOM)
    )

/**
 * An immutable set of [BlockSide] values stored as a bitmask in a single [Byte].
 *
 * Bit layout (MSB to LSB): `- | - | BOTTOM | TOP | RIGHT | BACK | LEFT | FRONT`
 */
@JvmInline
value class BlockSideSet(val data: Byte) {
    
    /**
     * Creates a new [BlockSideSet] with the sides that are set to `true`.
     */
    constructor(
        front: Boolean = false,
        left: Boolean = false,
        back: Boolean = false,
        right: Boolean = false,
        top: Boolean = false,
        bottom: Boolean = false
    ) : this(
        ((if (front) 0b000001 else 0) or
            (if (left) 0b000010 else 0) or
            (if (back) 0b000100 else 0) or
            (if (right) 0b001000 else 0) or
            (if (top) 0b010000 else 0) or
            (if (bottom) 0b100000 else 0)).toByte()
    )
    
    /**
     * Checks whether [side] is contained in this set.
     */
    operator fun contains(side: BlockSide): Boolean =
        (data.toInt() and (1 shl side.ordinal)) != 0
    
    /**
     * Checks whether this set contains no sides.
     */
    fun isEmpty(): Boolean =
        data == 0.toByte()
    
    /**
     * Checks whether this set contains at least one side.
     */
    fun isNotEmpty(): Boolean =
        data != 0.toByte()
    
    /**
     * Returns a new [BlockSideSet] that is the union of this set and [other].
     */
    operator fun plus(other: BlockSideSet): BlockSideSet =
        BlockSideSet((this.data.toInt() or other.data.toInt()).toByte())
    
    /**
     * Returns a new [BlockSideSet] with [side] added.
     */
    operator fun plus(side: BlockSide): BlockSideSet =
        BlockSideSet((data.toInt() or (1 shl side.ordinal)).toByte())
    
    /**
     * Returns a new [BlockSideSet] with all sides in [other] removed.
     */
    operator fun minus(other: BlockSideSet): BlockSideSet =
        BlockSideSet((this.data.toInt() and other.data.toInt().inv()).toByte())
    
    /**
     * Returns a new [BlockSideSet] with [side] removed.
     */
    operator fun minus(side: BlockSide): BlockSideSet =
        BlockSideSet((data.toInt() and (1 shl side.ordinal).inv()).toByte())
    
    /**
     * Returns a new [BlockSideSet] that is the intersection of this set and [other].
     */
    infix fun and(other: BlockSideSet): BlockSideSet =
        BlockSideSet((this.data.toInt() and other.data.toInt()).toByte())
    
    /**
     * Runs [action] for each [BlockSide] contained in this set.
     */
    inline fun forEach(action: (BlockSide) -> Unit) {
        if ((data.toInt() and 0b000001) != 0)
            action(BlockSide.FRONT)
        if ((data.toInt() and 0b000010) != 0)
            action(BlockSide.LEFT)
        if ((data.toInt() and 0b000100) != 0)
            action(BlockSide.BACK)
        if ((data.toInt() and 0b001000) != 0)
            action(BlockSide.RIGHT)
        if ((data.toInt() and 0b010000) != 0)
            action(BlockSide.TOP)
        if ((data.toInt() and 0b100000) != 0)
            action(BlockSide.BOTTOM)
    }
    
    /**
     * Returns a list of results from applying [transform] to each [BlockSide] in this set.
     */
    inline fun <V> map(transform: (BlockSide) -> V): List<V> {
        val list = ArrayList<V>(6)
        forEach { list.add(transform(it)) }
        return list
    }
    
    /**
     * Returns a [BlockSideMap] where each side in this set is associated with the value
     * produced by [valueSelector], and all other sides are `null`.
     */
    inline fun <V> associateWith(valueSelector: (BlockSide) -> V): BlockSideMap<V?> {
        if (data == 0.toByte())
            return BlockSideMap.NULL
        
        return BlockSideMap(
            front = if (BlockSide.FRONT in this) valueSelector(BlockSide.FRONT) else null,
            left = if (BlockSide.LEFT in this) valueSelector(BlockSide.LEFT) else null,
            back = if (BlockSide.BACK in this) valueSelector(BlockSide.BACK) else null,
            right = if (BlockSide.RIGHT in this) valueSelector(BlockSide.RIGHT) else null,
            top = if (BlockSide.TOP in this) valueSelector(BlockSide.TOP) else null,
            bottom = if (BlockSide.BOTTOM in this) valueSelector(BlockSide.BOTTOM) else null
        )
    }
    
    /**
     * Converts this [BlockSideSet] to a [CubeFaceSet] given the [front] direction.
     */
    fun toCubeFaceSet(front: BlockFace): CubeFaceSet {
        var data = 0
        if (BlockSide.FRONT in this)
            data = data or (1 shl BlockSide.FRONT.getBlockFace(front).ordinal)
        if (BlockSide.LEFT in this)
            data = data or (1 shl BlockSide.LEFT.getBlockFace(front).ordinal)
        if (BlockSide.BACK in this)
            data = data or (1 shl BlockSide.BACK.getBlockFace(front).ordinal)
        if (BlockSide.RIGHT in this)
            data = data or (1 shl BlockSide.RIGHT.getBlockFace(front).ordinal)
        if (BlockSide.TOP in this)
            data = data or (1 shl BlockFace.UP.ordinal)
        if (BlockSide.BOTTOM in this)
            data = data or (1 shl BlockFace.DOWN.ordinal)
        return CubeFaceSet(data.toByte())
    }
    
    companion object {
        
        /** A [BlockSideSet] containing all sides. */
        val ALL = BlockSideSet(-1)
        
        /** An empty [BlockSideSet]. */
        val NONE = BlockSideSet(0)
        
    }
    
}
