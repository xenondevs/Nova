package xyz.xenondevs.nova.world.block.state.model

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
internal data object ModelLessBlockModelProvider : BlockModelProvider<BlockData> {
    
    override fun set(pos: BlockPos, info: BlockData) {
        pos.block.setBlockData(info, true)
    }
    
    override fun remove(pos: BlockPos) {
        pos.block.type = Material.AIR
    }
    
    override fun replace(pos: BlockPos, prevInfo: BlockData, newInfo: BlockData) {
        if (prevInfo == newInfo)
            return
        
        remove(pos)
        set(pos, newInfo)
    }
    
    override fun load(pos: BlockPos, info: BlockData) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
