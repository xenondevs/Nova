package xyz.xenondevs.nova.world.block.state.property

import net.minecraft.resources.ResourceLocation
import org.bukkit.Axis
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.calculateYaw
import xyz.xenondevs.nova.util.calculateYawPitch
import xyz.xenondevs.nova.world.block.state.property.impl.BooleanProperty
import xyz.xenondevs.nova.world.block.state.property.impl.EnumProperty
import xyz.xenondevs.nova.world.block.state.property.impl.IntProperty

object DefaultBlockStateProperties {
    
    /**
     * A property for the [BlockFace] a block is facing.
     */
    val FACING: EnumProperty<BlockFace> = EnumProperty(ResourceLocation("nova", "facing"))
    
    /**
     * A property for the [Axis] a block is aligned to.
     */
    val AXIS: EnumProperty<Axis> = EnumProperty(ResourceLocation("nova", "axis"))
    
    /**
     * A property for the redstone powered state of a block.
     */
    val POWERED: BooleanProperty = BooleanProperty(ResourceLocation("nova", "powered"))
    
    /**
     * A property the [Instrument] of a note block.
     */
    val INSTRUMENT: EnumProperty<Instrument> = EnumProperty(ResourceLocation("nova", "instrument"))
    
    /**
     * A property for the note value of a note block.
     */
    val NOTE: IntProperty = IntProperty(ResourceLocation("nova", "note"))
    
}

object DefaultScopedBlockStateProperties {
    
    /**
     * A scope for [DefaultBlockStateProperties.FACING], limited to the four horizontal directions
     * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH] and [BlockFace.WEST].
     */
    val FACING_HORIZONTAL: ScopedBlockStateProperty<BlockFace> =
        DefaultBlockStateProperties.FACING.scope(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toCartesianFace(it) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.FACING], limited to the two vertical directions [BlockFace.UP] and [BlockFace.DOWN].
     */
    val FACING_VERTICAL: ScopedBlockStateProperty<BlockFace> =
        DefaultBlockStateProperties.FACING.scope(BlockFace.UP, BlockFace.DOWN) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]?.calculateYawPitch()
                ?.let { (_, pitch) -> if (pitch < 0) BlockFace.UP else BlockFace.DOWN }
                ?: BlockFace.UP
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.FACING], limited to the six cartesian directions
     * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH], [BlockFace.WEST], [BlockFace.UP] and [BlockFace.DOWN].
     */
    val FACING_CARTESIAN: ScopedBlockStateProperty<BlockFace> =
        DefaultBlockStateProperties.FACING.scope(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
        ) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYawPitch()
                ?.let { (yaw, pitch) -> BlockFaceUtils.toCartesianFace(yaw, pitch) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.FACING], limited to the sixteen cardinal directions
     * [BlockFace.NORTH], [BlockFace.NORTH_NORTH_EAST], [BlockFace.NORTH_EAST], [BlockFace.EAST_NORTH_EAST],
     * [BlockFace.EAST], [BlockFace.EAST_SOUTH_EAST], [BlockFace.SOUTH_EAST], [BlockFace.SOUTH_SOUTH_EAST],
     * [BlockFace.SOUTH], [BlockFace.SOUTH_SOUTH_WEST], [BlockFace.SOUTH_WEST], [BlockFace.WEST_SOUTH_WEST],
     * [BlockFace.WEST], [BlockFace.WEST_NORTH_WEST], [BlockFace.NORTH_WEST] and [BlockFace.NORTH_NORTH_WEST].
     */
    val FACING_ROTATION: ScopedBlockStateProperty<BlockFace> =
        DefaultBlockStateProperties.FACING.scope(
            BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST
        ) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toRotation(it) }
                ?.oppositeFace
                ?: BlockFace.NORTH
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.AXIS] for all three axes [Axis.X], [Axis.Y] and [Axis.Z].
     */
    val AXIS: ScopedBlockStateProperty<Axis> =
        DefaultBlockStateProperties.AXIS.scope(Axis.X, Axis.Y, Axis.Z) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYawPitch()
                ?.let { (yaw, pitch) -> BlockFaceUtils.toAxis(yaw, pitch) }
                ?: Axis.Y
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.AXIS] for the two horizontal axes [Axis.X] and [Axis.Z].
     */
    val AXIS_HORIZONTAL: ScopedBlockStateProperty<Axis> =
        DefaultBlockStateProperties.AXIS.scope(Axis.X, Axis.Z) { ctx ->
            ctx[ContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toAxis(it) }
                ?: Axis.X
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.POWERED].
     */
    val POWERED: ScopedBlockStateProperty<Boolean> =
        DefaultBlockStateProperties.POWERED.scope { ctx -> ctx.getOrThrow(ContextParamTypes.BLOCK_POS).block.isBlockIndirectlyPowered }
    
    /**
     * A scope for [DefaultBlockStateProperties.INSTRUMENT] for all [Instruments][Instrument].
     */
    val INSTRUMENT: ScopedBlockStateProperty<Instrument> =
        DefaultBlockStateProperties.INSTRUMENT.scope { ctx -> Instrument.determineInstrument(ctx.getOrThrow(ContextParamTypes.BLOCK_POS)) }
    
    /**
     * A scope for [DefaultBlockStateProperties.NOTE] for all 25 note block notes.
     */
    val NOTE: ScopedBlockStateProperty<Int> =
        DefaultBlockStateProperties.NOTE.scope(0..24) { 0 }
    
}