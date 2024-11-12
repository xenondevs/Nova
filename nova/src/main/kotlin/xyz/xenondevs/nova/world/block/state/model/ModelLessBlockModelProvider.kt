package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
internal data object ModelLessBlockModelProvider : BlockModelProvider<BlockState> {
    
    override fun set(pos: BlockPos, info: BlockState, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(info)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info)
            }
        }
    }
    
    override fun replace(pos: BlockPos, info: BlockState, method: BlockUpdateMethod) {
        set(pos, info, method)
    }
    
    override fun load(pos: BlockPos, info: BlockState) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
