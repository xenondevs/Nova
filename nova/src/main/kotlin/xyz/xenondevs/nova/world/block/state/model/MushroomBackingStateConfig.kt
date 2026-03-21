package xyz.xenondevs.nova.world.block.state.model

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries

internal class RedMushroomBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, BlockTypeEntries.RED_MUSHROOM_BLOCK) {
    
    override val type = RedMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<RedMushroomBackingStateConfig>(
        ::RedMushroomBackingStateConfig,
        "red_mushroom_block"
    )
    
}

internal class BrownMushroomBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, BlockTypeEntries.BROWN_MUSHROOM_BLOCK) {
    
    override val type = BrownMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<BrownMushroomBackingStateConfig>(
        ::BrownMushroomBackingStateConfig,
        "brown_mushroom_block"
    )
    
}

internal class MushroomStemBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, BlockTypeEntries.MUSHROOM_STEM) {
    
    override val type = MushroomStemBackingStateConfig
    
    companion object : SidedBackingStateConfigType<MushroomStemBackingStateConfig>(
        ::MushroomStemBackingStateConfig,
        "mushroom_stem"
    )
    
}