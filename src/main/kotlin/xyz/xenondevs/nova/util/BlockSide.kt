package xyz.xenondevs.nova.util

import org.bukkit.block.BlockFace
import kotlin.math.roundToInt

enum class BlockSide(private val rotation: Int, private val blockFace: BlockFace) {
    
    FRONT(0, BlockFace.SOUTH),
    LEFT(1, BlockFace.WEST),
    BACK(2, BlockFace.NORTH),
    RIGHT(3, BlockFace.EAST),
    TOP(-1, BlockFace.UP),
    BOTTOM(-1, BlockFace.DOWN);
    
    fun getBlockFace(yaw: Float): BlockFace {
        if (rotation == -1) return blockFace
        val rot = ((yaw / 90.0).roundToInt() + rotation) % 4
        return values().find { it.rotation == rot }!!.blockFace
    }
    
}