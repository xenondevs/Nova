package xyz.xenondevs.nova.world.block.state.model

import it.unimi.dsi.fastutil.ints.IntArraySet
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.TripWireBlock
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.toIntArray

internal class TripwireBackingStateConfig(
    override val type: TripwireBackingStateConfigType,
    north: Boolean, east: Boolean, south: Boolean, west: Boolean,
    disarmed: Boolean, powered: Boolean
) : BackingStateConfig() {
    
    override val id = 0 or
        (powered.intValue shl 5) or
        (disarmed.intValue shl 4) or
        (west.intValue shl 3) or
        (south.intValue shl 2) or
        (east.intValue shl 1) or
        north.intValue
    
    override val variantString = "north=$north,east=$east,south=$south,west=$west,attached=${type.attached},disarmed=$disarmed,powered=$powered"
    
    override val vanillaBlockState = Blocks.TRIPWIRE.defaultBlockState()
        .setValue(TripWireBlock.NORTH, north)
        .setValue(TripWireBlock.EAST, east)
        .setValue(TripWireBlock.SOUTH, south)
        .setValue(TripWireBlock.WEST, west)
        .setValue(TripWireBlock.ATTACHED, type.attached)
        .setValue(TripWireBlock.DISARMED, disarmed)
        .setValue(TripWireBlock.POWERED, powered)
    
    constructor(
        type: TripwireBackingStateConfigType,
        faces: Set<BlockFace>, disarmed: Boolean, powered: Boolean
    ) : this(
        type,
        BlockFace.NORTH in faces,
        BlockFace.EAST in faces,
        BlockFace.SOUTH in faces,
        BlockFace.WEST in faces,
        disarmed, powered
    )
    
}

internal abstract class TripwireBackingStateConfigType private constructor(
    val attached: Boolean
) : BackingStateConfigType<TripwireBackingStateConfig>(63, "tripwire") {
    
    override val blockedIds = IntArraySet((0..15).toIntArray())
    
    override fun of(id: Int): TripwireBackingStateConfig {
        return TripwireBackingStateConfig(
            this,
            (id and 1) == 1,
            (id and 2) == 2,
            (id and 4) == 4,
            (id and 8) == 8,
            (id and 16) == 16,
            (id and 32) == 32
        )
    }
    
    override fun of(properties: Map<String, String>): TripwireBackingStateConfig {
        return TripwireBackingStateConfig(
            this,
            properties["north"]?.toBoolean() ?: false,
            properties["east"]?.toBoolean() ?: false,
            properties["south"]?.toBoolean() ?: false,
            properties["west"]?.toBoolean() ?: false,
            properties["disarmed"]?.toBoolean() ?: false,
            properties["powered"]?.toBoolean() ?: false
        )
    }
    
    object Unattached : TripwireBackingStateConfigType(false)
    object Attached : TripwireBackingStateConfigType(true)
    
}