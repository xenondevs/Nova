package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.serialization.kotlinx.ModelLessBlockModelProviderSerializer
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
@Serializable(ModelLessBlockModelProviderSerializer::class)
@SerialName("model_less")
internal class ModelLessBlockModelProvider(
    val infoProvider: Provider<BlockData>
) : BlockModelProvider {
    
    val info: BlockData by infoProvider
    
    override fun set(pos: BlockPos, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(info.nmsBlockState)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info.nmsBlockState)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info.nmsBlockState)
            }
        }
    }
    
    override fun replace(pos: BlockPos, method: BlockUpdateMethod) {
        set(pos, method)
    }
    
    override fun load(pos: BlockPos) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
