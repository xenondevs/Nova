package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.util.nmsBlockState

class BlockDisplayMetadata : DisplayMetadata() {
    
    var blockState: BlockData by entry(23, EntityDataSerializers.BLOCK_STATE, Material.AIR.createBlockData()) { it.nmsBlockState }
    
}