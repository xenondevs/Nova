package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateSerializer
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
@Serializable
@SerialName("model_less")
internal class ModelLessBlockModelProvider(
    @Serializable(with = BlockStateSerializer::class)
    val info: BlockState
) : BlockModelProvider {
    
    override fun set(pos: BlockPos, method: BlockUpdateMethod) {
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(info)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(info)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(info)
            }
        }
    }
    
    override fun replace(pos: BlockPos, method: BlockUpdateMethod) {
        set(pos, method)
    }
    
    override fun load(pos: BlockPos) = Unit
    override fun unload(pos: BlockPos) = Unit
    
}
