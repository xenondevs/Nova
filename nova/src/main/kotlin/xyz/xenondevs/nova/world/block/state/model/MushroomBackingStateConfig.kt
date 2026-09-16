package xyz.xenondevs.nova.world.block.state.model

import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.util.CubeFaceSet

internal class RedMushroomBackingStateConfig(faces: CubeFaceSet) : SidedBackingStateConfig(faces, BlockTypeEntries.RED_MUSHROOM_BLOCK) {
    
    override val type = RedMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<RedMushroomBackingStateConfig>(
        ::RedMushroomBackingStateConfig,
        "red_mushroom_block"
    )
    
}

internal class BrownMushroomBackingStateConfig(faces: CubeFaceSet) : SidedBackingStateConfig(faces, BlockTypeEntries.BROWN_MUSHROOM_BLOCK) {
    
    override val type = BrownMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<BrownMushroomBackingStateConfig>(
        ::BrownMushroomBackingStateConfig,
        "brown_mushroom_block"
    )
    
}

internal class MushroomStemBackingStateConfig(faces: CubeFaceSet) : SidedBackingStateConfig(faces, BlockTypeEntries.MUSHROOM_STEM) {
    
    override val type = MushroomStemBackingStateConfig
    
    companion object : SidedBackingStateConfigType<MushroomStemBackingStateConfig>(
        ::MushroomStemBackingStateConfig,
        "mushroom_stem"
    )
    
}
