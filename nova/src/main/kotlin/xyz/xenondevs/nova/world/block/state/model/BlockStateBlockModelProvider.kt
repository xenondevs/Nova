package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that uses vanilla block states to display the block model.
 */
@Serializable
@SerialName("state_backed")
internal class BackingStateBlockModelProvider(val info: BackingStateConfig) : BlockModelProvider {
    
    override fun set(pos: BlockPos, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(info.vanillaBlockState)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info.vanillaBlockState)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info.vanillaBlockState)
            }
        }
    }
    
    override fun replace(pos: BlockPos, method: BlockUpdateMethod) {
        set(pos, method)
    }
    
    override fun load(pos: BlockPos) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
