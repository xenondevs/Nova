package xyz.xenondevs.nova.world.block.state.property

import net.kyori.adventure.key.Key
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.Axis
import org.bukkit.Fluid
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.block.data.CraftBlockData
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.calculateYaw
import xyz.xenondevs.nova.util.calculateYawPitch

object DefaultBlockStateProperties {
    
    /**
     * A property for the waterlogged state of a block.
     */
    val WATERLOGGED: BooleanProperty = BooleanProperty(
        BlockStateProperties.WATERLOGGED
    ) { ctx ->
        val pos = ctx[BlockPlace.BLOCK]
        pos.world.getFluidData(pos.x, pos.y, pos.z).fluidType == Fluid.WATER
    }
    
    /**
     * A property for the redstone powered state of a block.
     */
    val POWERED: BooleanProperty = BooleanProperty(BlockStateProperties.POWERED) { ctx ->
        ctx[BlockPlace.BLOCK].isBlockIndirectlyPowered
    }
    
    /**
     * A property for facing limited to the four horizontal directions
     * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH] and [BlockFace.WEST].
     */
    val FACING_HORIZONTAL: BlockStateProperty<BlockFace> =
        MappedProperty(
            BlockStateProperties.HORIZONTAL_FACING,
            BlockFace.NORTH,
            CraftBlock::notchToBlockFace,
            { CraftBlock.blockFaceToNotch(it) ?: throw IllegalArgumentException("Invalid block face: $it") }
        ) { ctx ->
            ctx[BlockPlace.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toCartesianFace(it) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A property for facing limited to the two vertical directions [BlockFace.UP] and [BlockFace.DOWN].
     */
    val FACING_VERTICAL: BlockStateProperty<BlockFace> =
        EnumProperty(Key.key("nova", "facing"), BlockFace.UP, BlockFace.DOWN) { ctx ->
            ctx[BlockPlace.SOURCE_DIRECTION]?.calculateYawPitch()
                ?.let { [_, pitch] -> if (pitch < 0) BlockFace.UP else BlockFace.DOWN }
                ?: BlockFace.UP
        }
    
    /**
     * A property for facing limited to the six cartesian directions
     * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH], [BlockFace.WEST], [BlockFace.UP] and [BlockFace.DOWN].
     */
    val FACING_CARTESIAN: BlockStateProperty<BlockFace> =
        MappedProperty(
            BlockStateProperties.FACING,
            BlockFace.NORTH,
            CraftBlock::notchToBlockFace,
            { CraftBlock.blockFaceToNotch(it) ?: throw IllegalArgumentException("Invalid block face: $it") }
        ) { ctx ->
            ctx[BlockPlace.SOURCE_DIRECTION]
                ?.calculateYawPitch()
                ?.let { [yaw, pitch] -> BlockFaceUtils.toCartesianFace(yaw, pitch) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A property for facing limited to the sixteen cardinal directions
     * [BlockFace.NORTH], [BlockFace.NORTH_NORTH_EAST], [BlockFace.NORTH_EAST], [BlockFace.EAST_NORTH_EAST],
     * [BlockFace.EAST], [BlockFace.EAST_SOUTH_EAST], [BlockFace.SOUTH_EAST], [BlockFace.SOUTH_SOUTH_EAST],
     * [BlockFace.SOUTH], [BlockFace.SOUTH_SOUTH_WEST], [BlockFace.SOUTH_WEST], [BlockFace.WEST_SOUTH_WEST],
     * [BlockFace.WEST], [BlockFace.WEST_NORTH_WEST], [BlockFace.NORTH_WEST] and [BlockFace.NORTH_NORTH_WEST].
     */
    val FACING_ROTATION: BlockStateProperty<BlockFace> =
        MappedProperty(
            BlockStateProperties.ROTATION_16,
            BlockFace.NORTH,
            { CraftBlockData.ROTATION_CYCLE[it] },
            { CraftBlockData.ROTATION_CYCLE.indexOf(it).also { index -> require(index >= 0) } }
        ) { ctx ->
            ctx[BlockPlace.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toFace(it) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A property for all three axes [Axis.X], [Axis.Y] and [Axis.Z].
     */
    val AXIS: BlockStateProperty<Axis> =
        MappedProperty(
            BlockStateProperties.AXIS,
            Axis.Y,
            { Axis.valueOf(it.name) },
            { Direction.Axis.valueOf(it.name) }
        ) { ctx ->
            ctx[BlockPlace.CLICKED_BLOCK_FACE]?.axis ?: Axis.Y
        }
    
    /**
     * A property for the two horizontal axes [Axis.X] and [Axis.Z].
     */
    val AXIS_HORIZONTAL: BlockStateProperty<Axis> =
        MappedProperty(
            BlockStateProperties.HORIZONTAL_AXIS,
            Axis.X,
            { Axis.valueOf(it.name) },
            { Direction.Axis.valueOf(it.name) }
        ) { ctx ->
            ctx[BlockPlace.CLICKED_BLOCK_FACE]?.axis ?: Axis.X
        }
    
    val FACING_PROPERTIES = listOf(
        FACING_HORIZONTAL,
        FACING_VERTICAL,
        FACING_CARTESIAN,
        FACING_ROTATION
    )
    
    val AXIS_PROPERTIES = listOf(
        AXIS,
        AXIS_HORIZONTAL
    )
    
}