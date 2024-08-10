package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import org.bukkit.block.BlockFace

internal class RedMushroomBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, Blocks.RED_MUSHROOM_BLOCK) {
    
    override val type = RedMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<RedMushroomBackingStateConfig>(
        ::RedMushroomBackingStateConfig,
        "red_mushroom_block"
    )
    
}

internal class BrownMushroomBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, Blocks.BROWN_MUSHROOM_BLOCK) {
    
    override val type = BrownMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<BrownMushroomBackingStateConfig>(
        ::BrownMushroomBackingStateConfig, 
        "brown_mushroom_block"
    )
    
}

internal class MushroomStemBackingStateConfig(faces: Set<BlockFace>) : SidedBackingStateConfig(faces, Blocks.MUSHROOM_STEM) {
    
    override val type = MushroomStemBackingStateConfig
    
    companion object : SidedBackingStateConfigType<MushroomStemBackingStateConfig>(
        ::MushroomStemBackingStateConfig,
        "mushroom_stem"
    )
    
}