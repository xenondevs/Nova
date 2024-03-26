package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import org.bukkit.Material
import org.bukkit.block.BlockFace

internal class RedMushroomBackingStateConfig(faces: List<BlockFace>) : SidedBackingStateConfig(faces, Blocks.RED_MUSHROOM_BLOCK) {
    
    override val type = RedMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<RedMushroomBackingStateConfig>(::RedMushroomBackingStateConfig) {
        override val fileName = "red_mushroom_block"
        override val material = Material.RED_MUSHROOM_BLOCK
    }
    
}

internal class BrownMushroomBackingStateConfig(faces: List<BlockFace>) : SidedBackingStateConfig(faces, Blocks.BROWN_MUSHROOM_BLOCK) {
    
    override val type = BrownMushroomBackingStateConfig
    
    companion object : SidedBackingStateConfigType<BrownMushroomBackingStateConfig>(::BrownMushroomBackingStateConfig) {
        override val fileName = "brown_mushroom_block"
        override val material = Material.BROWN_MUSHROOM_BLOCK
    }
    
}

internal class MushroomStemBackingStateConfig(faces: List<BlockFace>) : SidedBackingStateConfig(faces, Blocks.MUSHROOM_STEM) {
    
    override val type = MushroomStemBackingStateConfig
    
    companion object : SidedBackingStateConfigType<MushroomStemBackingStateConfig>(::MushroomStemBackingStateConfig) {
        override val fileName = "mushroom_stem"
        override val material = Material.MUSHROOM_STEM
    }
    
}