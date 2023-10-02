package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class BlockDisplayMetadata : DisplayMetadata() {
    
    var blockState: BlockState by entry(23, EntityDataSerializers.BLOCK_STATE, Blocks.AIR.defaultBlockState())
    
}