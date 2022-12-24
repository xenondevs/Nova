package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.world.fakeentity.metadata.MetadataSerializers

class FallingBlockMetadata : EntityMetadata() {
    
    var spawnPosition by entry(8, MetadataSerializers.BLOCK_POS, BlockPos.ZERO)
    
}