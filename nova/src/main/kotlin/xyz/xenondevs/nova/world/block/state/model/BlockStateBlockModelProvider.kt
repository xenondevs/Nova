package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.world.BlockPos

private val AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState()

/**
 * A block model provider that uses vanilla block states to display the block model.
 */
internal data object BackingStateBlockModelProvider : BlockModelProvider<BackingStateConfig> {
    
    override fun set(pos: BlockPos, info: BackingStateConfig) {
        pos.setBlockStateNoUpdate(info.vanillaBlockState)
    }
    
    override fun remove(pos: BlockPos) {
        pos.setBlockStateNoUpdate(AIR_BLOCK_STATE)
    }
    
    override fun replace(pos: BlockPos, prevInfo: BackingStateConfig, newInfo: BackingStateConfig) {
        remove(pos)
        set(pos, newInfo)
    }
    
    override fun load(pos: BlockPos, info: BackingStateConfig) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
