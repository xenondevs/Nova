package xyz.xenondevs.nova.world.block.state.property

import net.minecraft.resources.ResourceLocation
import org.bukkit.Axis
import org.bukkit.Fluid
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.Instrument
import xyz.xenondevs.nova.util.calculateYaw
import xyz.xenondevs.nova.util.calculateYawPitch
import xyz.xenondevs.nova.world.block.behavior.LeavesBehavior
import xyz.xenondevs.nova.world.block.state.property.impl.BooleanProperty
import xyz.xenondevs.nova.world.block.state.property.impl.EnumProperty
import xyz.xenondevs.nova.world.block.state.property.impl.IntProperty

object DefaultBlockStateProperties {
    
    /**
     * A property for the [BlockFace] a block is facing.
     */
    val FACING: EnumProperty<BlockFace> = EnumProperty(ResourceLocation.fromNamespaceAndPath("nova", "facing"))
    
    /**
     * A property for the [Axis] a block is aligned to.
     */
    val AXIS: EnumProperty<Axis> = EnumProperty(ResourceLocation.fromNamespaceAndPath("nova", "axis"))
    
    /**
     * A property for the waterlogged state of a block.
     */
    val WATERLOGGED: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "waterlogged"))
    
    /**
     * A property for the redstone powered state of a block.
     */
    val POWERED: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "powered"))
    
    internal val NOTE_BLOCK_INSTRUMENT: EnumProperty<Instrument> = EnumProperty(ResourceLocation.fromNamespaceAndPath("nova", "instrument"))
    internal val NOTE_BLOCK_NOTE: IntProperty = IntProperty(ResourceLocation.fromNamespaceAndPath("nova", "note"))
    internal val LEAVES_DISTANCE: IntProperty = IntProperty(ResourceLocation.fromNamespaceAndPath("nova", "distance"))
    internal val LEAVES_PERSISTENT: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "persistent"))
    internal val TRIPWIRE_NORTH: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "north"))
    internal val TRIPWIRE_EAST: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "east"))
    internal val TRIPWIRE_SOUTH: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "south"))
    internal val TRIPWIRE_WEST: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "west"))
    internal val TRIPWIRE_ATTACHED: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "attached"))
    internal val TRIPWIRE_DISARMED: BooleanProperty = BooleanProperty(ResourceLocation.fromNamespaceAndPath("nova", "disarmed"))
    
}

object DefaultScopedBlockStateProperties {
    
    /**
     * A scope for [DefaultBlockStateProperties.FACING], limited to the four horizontal directions
     * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH] and [BlockFace.WEST].
     */
    val FACING_HORIZONTAL: ScopedBlockStateProperty<BlockFace> =
        DefaultBlockStateProperties.FACING.scope(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST) { ctx ->
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]
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
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]?.calculateYawPitch()
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
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]
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
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]
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
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYawPitch()
                ?.let { (yaw, pitch) -> BlockFaceUtils.toAxis(yaw, pitch) }
                ?: Axis.Y
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.AXIS] for the two horizontal axes [Axis.X] and [Axis.Z].
     */
    val AXIS_HORIZONTAL: ScopedBlockStateProperty<Axis> =
        DefaultBlockStateProperties.AXIS.scope(Axis.X, Axis.Z) { ctx ->
            ctx[DefaultContextParamTypes.SOURCE_DIRECTION]
                ?.calculateYaw()
                ?.let { BlockFaceUtils.toAxis(it) }
                ?: Axis.X
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.WATERLOGGED] that is initialized based on the fluid state of the block.
     */
    val WATERLOGGED: ScopedBlockStateProperty<Boolean> =
        DefaultBlockStateProperties.WATERLOGGED.scope { ctx ->
            val pos = ctx.getOrThrow(DefaultContextParamTypes.BLOCK_POS)
            pos.world.getFluidData(pos.x, pos.y, pos.z).fluidType == Fluid.WATER
        }
    
    /**
     * A scope for [DefaultBlockStateProperties.POWERED].
     */
    val POWERED: ScopedBlockStateProperty<Boolean> =
        DefaultBlockStateProperties.POWERED.scope { ctx ->
            ctx.getOrThrow(DefaultContextParamTypes.BLOCK_POS).block.isBlockIndirectlyPowered
        }
    
    internal val NOTE_BLOCK_INSTRUMENT: ScopedBlockStateProperty<Instrument> =
        DefaultBlockStateProperties.NOTE_BLOCK_INSTRUMENT.scope { ctx ->
            Instrument.determineInstrument(ctx.getOrThrow(DefaultContextParamTypes.BLOCK_POS))
        }
    
    internal val NOTE_BLOCK_NOTE: ScopedBlockStateProperty<Int> =
        DefaultBlockStateProperties.NOTE_BLOCK_NOTE.scope(0..24) { 0 }

    internal val LEAVES_DISTANCE: ScopedBlockStateProperty<Int> =
        DefaultBlockStateProperties.LEAVES_DISTANCE.scope(1..7) { ctx ->
            LeavesBehavior.calculateDistance(ctx.getOrThrow(DefaultContextParamTypes.BLOCK_POS))
        }

    internal val LEAVES_PERSISTENT: ScopedBlockStateProperty<Boolean> =
        DefaultBlockStateProperties.LEAVES_PERSISTENT.scope { ctx ->
            ctx[DefaultContextParamTypes.SOURCE_UUID] != null
        }
    
    internal val TRIPWIRE_NORTH: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_NORTH.scope { false }
    internal val TRIPWIRE_EAST: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_EAST.scope { false }
    internal val TRIPWIRE_SOUTH: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_SOUTH.scope { false }
    internal val TRIPWIRE_WEST: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_WEST.scope { false }
    internal val TRIPWIRE_ATTACHED: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_ATTACHED.scope { true }
    internal val TRIPWIRE_DISARMED: ScopedBlockStateProperty<Boolean> = DefaultBlockStateProperties.TRIPWIRE_DISARMED.scope { false }
    
}