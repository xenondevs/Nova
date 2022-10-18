package xyz.xenondevs.nova.data.resources.model.blockstate

import net.minecraft.world.level.block.Blocks
import org.bukkit.block.BlockFace

internal class RedMushroomBlockStateConfig(faces: List<BlockFace>) : SidedBlockStateConfig(faces, Blocks.RED_MUSHROOM_BLOCK) {
    
    override val type = RedMushroomBlockStateConfig
    
    companion object : SidedBlockStateConfigType<RedMushroomBlockStateConfig>(::RedMushroomBlockStateConfig) {
        override val fileName = "red_mushroom_block"
    }
    
}

internal class BrownMushroomBlockStateConfig(faces: List<BlockFace>) : SidedBlockStateConfig(faces, Blocks.BROWN_MUSHROOM_BLOCK) {
    
    override val type = BrownMushroomBlockStateConfig
    
    companion object : SidedBlockStateConfigType<BrownMushroomBlockStateConfig>(::BrownMushroomBlockStateConfig) {
        override val fileName = "brown_mushroom_block"
    }
    
}

internal class MushroomStemBlockStateConfig(faces: List<BlockFace>) : SidedBlockStateConfig(faces, Blocks.MUSHROOM_STEM) {
    
    override val type = MushroomStemBlockStateConfig
    
    companion object : SidedBlockStateConfigType<MushroomStemBlockStateConfig>(::MushroomStemBlockStateConfig) {
        override val fileName = "mushroom_stem"
    }
    
}