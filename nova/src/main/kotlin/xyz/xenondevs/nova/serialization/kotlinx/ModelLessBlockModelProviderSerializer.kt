package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider

internal object ModelLessBlockModelProviderSerializer : KSerializer<ModelLessBlockModelProvider> {
    
    override val descriptor = BlockStateSerializer.descriptor
    
    override fun serialize(encoder: Encoder, value: ModelLessBlockModelProvider) {
        BlockStateSerializer.serialize(encoder, value.info.nmsBlockState)
    }
    
    override fun deserialize(decoder: Decoder): ModelLessBlockModelProvider {
        return ModelLessBlockModelProvider(provider(BlockStateSerializer.deserialize(decoder).bukkitBlockData))
    }
    
}