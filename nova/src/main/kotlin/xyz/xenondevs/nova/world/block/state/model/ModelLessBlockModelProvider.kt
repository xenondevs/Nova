package xyz.xenondevs.nova.world.block.state.model

import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.LevelChunkSectionWrapper
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
internal data object ModelLessBlockModelProvider : BlockModelProvider<BlockData> {
    
    override fun set(pos: BlockPos, info: BlockData, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.block.blockData = info
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info.nmsBlockState)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info.nmsBlockState)
            }
        }
    }
    
    override fun replace(pos: BlockPos, info: BlockData, method: BlockUpdateMethod) {
        set(pos, info, method)
    }
    
    override fun load(pos: BlockPos, info: BlockData) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
