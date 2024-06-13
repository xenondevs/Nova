package xyz.xenondevs.nova.world.block.state.model

import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that uses vanilla block states to display the block model.
 */
internal data object BackingStateBlockModelProvider : BlockModelProvider<BackingStateConfig> {
    
    override fun set(pos: BlockPos, info: BackingStateConfig, method: BlockUpdateMethod) {
        when (method) {
            BlockUpdateMethod.DEFAULT -> pos.setBlockState(info.vanillaBlockState)
            BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info.vanillaBlockState)
            BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info.vanillaBlockState)
        }
    }
    
    override fun replace(pos: BlockPos, info: BackingStateConfig, method: BlockUpdateMethod) {
        set(pos, info, method)
    }
    
    override fun load(pos: BlockPos, info: BackingStateConfig) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
