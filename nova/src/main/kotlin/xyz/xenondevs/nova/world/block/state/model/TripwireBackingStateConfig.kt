package xyz.xenondevs.nova.world.block.state.model

import it.unimi.dsi.fastutil.ints.IntArraySet
import net.minecraft.world.level.block.TripWireBlock
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.toIntArray

internal class TripwireBackingStateConfig(
    override val type: TripwireBackingStateConfigType,
    north: Boolean, east: Boolean, south: Boolean, west: Boolean,
    disarmed: Boolean, powered: Boolean
) : BackingStateConfig() {
    
    override val waterlogged = false
    override val id = 0 or
        (powered.intValue shl 5) or
        (disarmed.intValue shl 4) or
        (west.intValue shl 3) or
        (south.intValue shl 2) or
        (east.intValue shl 1) or
        north.intValue
    
    override val variantMap = mapOf(
        "north" to "$north",
        "east" to "$east",
        "south" to "$south",
        "west" to "$west",
        "attached" to "${type.attached}",
        "disarmed" to "$disarmed",
        "powered" to "$powered"
    )
    
    override val blockType = BlockTypeEntries.TRIPWIRE
    override val vanillaBlockState: BlockState by blockType.map {
        it.nmsBlock.defaultBlockState
            .setValue(TripWireBlock.NORTH, north)
            .setValue(TripWireBlock.EAST, east)
            .setValue(TripWireBlock.SOUTH, south)
            .setValue(TripWireBlock.WEST, west)
            .setValue(TripWireBlock.ATTACHED, type.attached)
            .setValue(TripWireBlock.DISARMED, disarmed)
            .setValue(TripWireBlock.POWERED, powered)
    }
    
}

internal abstract class TripwireBackingStateConfigType private constructor(
    val attached: Boolean
) : BackingStateConfigType<TripwireBackingStateConfig>(63, "tripwire") {
    
    override val blockedIds = IntArraySet((0..15).toIntArray())
    override val properties = hashSetOf("north", "east", "south", "west", "disarmed", "powered")
    override val isWaterloggable = false
    
    override fun of(id: Int, waterlogged: Boolean): TripwireBackingStateConfig {
        if (waterlogged)
            throw UnsupportedOperationException("Tripwire cannot be waterlogged")
        
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
            properties["north"]?.toBoolean() == true,
            properties["east"]?.toBoolean() == true,
            properties["south"]?.toBoolean() == true,
            properties["west"]?.toBoolean() == true,
            properties["disarmed"]?.toBoolean() == true,
            properties["powered"]?.toBoolean() == true
        )
    }
    
    object Unattached : TripwireBackingStateConfigType(false)
    object Attached : TripwireBackingStateConfigType(true)
    
}