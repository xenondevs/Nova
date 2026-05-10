package xyz.xenondevs.nova.util

import org.bukkit.block.BlockFace

/**
 * Creates a [CubeFaceSet] containing each cube [BlockFace] for which [init] returns `true`.
 */
inline fun CubeFaceSet(init: (face: BlockFace) -> Boolean): CubeFaceSet =
    CubeFaceSet(
        north = init(BlockFace.NORTH),
        east = init(BlockFace.EAST),
        south = init(BlockFace.SOUTH),
        west = init(BlockFace.WEST),
        up = init(BlockFace.UP),
        down = init(BlockFace.DOWN)
    )

/**
 * An immutable set of [BlockFace] values (of the cube faces [BlockFace.NORTH], [BlockFace.EAST],
 * [BlockFace.SOUTH], [BlockFace.WEST], [BlockFace.UP], [BlockFace.DOWN])
 * stored as a bitmask in a single [Byte].
 *
 * Bit layout (MSB to LSB): `- | - | DOWN | UP | WEST | SOUTH | EAST | NORTH`
 */
@JvmInline
value class CubeFaceSet(val data: Byte) {
    
    /**
     * Checks whether [face] is contained in this set.
     */
    operator fun contains(face: BlockFace): Boolean =
        (data.toInt() and (1 shl face.ordinal)) != 0
    
    /**
     * Checks whether this set contains no faces.
     */
    fun isEmpty(): Boolean =
        data == 0.toByte()
    
    /**
     * Checks whether this set contains at least one face.
     */
    fun isNotEmpty(): Boolean =
        data != 0.toByte()
    
    /**
     * Returns a new [CubeFaceSet] that is the union of this set and [other].
     */
    operator fun plus(other: CubeFaceSet): CubeFaceSet =
        CubeFaceSet((this.data.toInt() or other.data.toInt()).toByte())
    
    /**
     * Returns a new [CubeFaceSet] with [face] added.
     */
    operator fun plus(face: BlockFace): CubeFaceSet =
        CubeFaceSet((data.toInt() or (1 shl face.ordinal)).toByte())
    
    /**
     * Returns a new [CubeFaceSet] with all faces in [other] removed.
     */
    operator fun minus(other: CubeFaceSet): CubeFaceSet =
        CubeFaceSet((this.data.toInt() and other.data.toInt().inv()).toByte())
    
    /**
     * Returns a new [CubeFaceSet] with [face] removed.
     */
    operator fun minus(face: BlockFace): CubeFaceSet =
        CubeFaceSet((data.toInt() and (1 shl face.ordinal).inv()).toByte())
    
    /**
     * Returns a new [CubeFaceSet] that is the intersection of this set and [other].
     */
    infix fun and(other: CubeFaceSet): CubeFaceSet =
        CubeFaceSet((this.data.toInt() and other.data.toInt()).toByte())
    
    /**
     * Runs [action] for each [BlockFace] contained in this set.
     */
    inline fun forEach(action: (BlockFace) -> Unit) {
        if ((data.toInt() and 0b000001) != 0)
            action(BlockFace.NORTH)
        if ((data.toInt() and 0b000010) != 0)
            action(BlockFace.EAST)
        if ((data.toInt() and 0b000100) != 0)
            action(BlockFace.SOUTH)
        if ((data.toInt() and 0b001000) != 0)
            action(BlockFace.WEST)
        if ((data.toInt() and 0b010000) != 0)
            action(BlockFace.UP)
        if ((data.toInt() and 0b100000) != 0)
            action(BlockFace.DOWN)
    }
    
    /**
     * Returns a list of results from applying [transform] to each [BlockFace] in this set.
     */
    inline fun <V> map(transform: (BlockFace) -> V): List<V> {
        val list = ArrayList<V>(6)
        forEach { list.add(transform(it)) }
        return list
    }
    
    /**
     * Returns a [CubeFaceMap] where each face in this set is associated with the value
     * produced by [valueSelector], and all other faces are `null`.
     */
    inline fun <V> associateWith(valueSelector: (BlockFace) -> V): CubeFaceMap<V?> {
        if (data == 0.toByte())
            return CubeFaceMap.NULL
        
        return CubeFaceMap(
            north = if (BlockFace.NORTH in this) valueSelector(BlockFace.NORTH) else null,
            east = if (BlockFace.EAST in this) valueSelector(BlockFace.EAST) else null,
            south = if (BlockFace.SOUTH in this) valueSelector(BlockFace.SOUTH) else null,
            west = if (BlockFace.WEST in this) valueSelector(BlockFace.WEST) else null,
            up = if (BlockFace.UP in this) valueSelector(BlockFace.UP) else null,
            down = if (BlockFace.DOWN in this) valueSelector(BlockFace.DOWN) else null
        )
    }
    
    /**
     * Converts this [CubeFaceSet] to a [BlockSideSet] given the [front] direction.
     */
    fun toBlockSideSet(front: BlockFace): BlockSideSet {
        if (data == 0.toByte())
            return BlockSideSet.NONE
        
        var data = 0
        if (BlockFace.NORTH in this)
            data = data or (1 shl BlockSide.of(front, BlockFace.NORTH).ordinal)
        if (BlockFace.EAST in this)
            data = data or (1 shl BlockSide.of(front, BlockFace.EAST).ordinal)
        if (BlockFace.SOUTH in this)
            data = data or (1 shl BlockSide.of(front, BlockFace.SOUTH).ordinal)
        if (BlockFace.WEST in this)
            data = data or (1 shl BlockSide.of(front, BlockFace.WEST).ordinal)
        if (BlockFace.UP in this)
            data = data or (1 shl BlockSide.TOP.ordinal)
        if (BlockFace.DOWN in this)
            data = data or (1 shl BlockSide.BOTTOM.ordinal)
        
        return BlockSideSet(data.toByte())
    }
    
    companion object {
        
        /** A [CubeFaceSet] containing all six faces. */
        val ALL = CubeFaceSet(-1)
        
        /** An empty [CubeFaceSet]. */
        val NONE = CubeFaceSet(0)
        
    }
    
}
