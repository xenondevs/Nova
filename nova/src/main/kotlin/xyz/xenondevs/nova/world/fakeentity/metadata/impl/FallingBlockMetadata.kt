package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataSerializers

class FallingBlockMetadata : EntityMetadata() {
    
    var spawnPosition: BlockPos by entry(8, EntityDataSerializers.BLOCK_POS, BlockPos.ZERO)
    
}