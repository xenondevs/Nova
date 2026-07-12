package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.serialization.kotlinx.ModelLessBlockModelProviderSerializer
import xyz.xenondevs.nova.util.nmsBlockState

/**
 * A block model provider that just places a vanilla block state that is not associated with any custom model.
 */
@Serializable(ModelLessBlockModelProviderSerializer::class)
@SerialName("model_less")
internal class ModelLessBlockModelProvider(
    val infoProvider: Provider<BlockData>
) : BlockModelProvider {
    val info: BlockData by infoProvider
    override val clientsideBlockState: BlockState
        get() = info.nmsBlockState
}