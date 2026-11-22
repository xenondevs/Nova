package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.nmsBlockState

internal object BlockDataSerializer : KSerializer<BlockData> {
    
    override val descriptor = BlockStateSerializer.descriptor
    
    override fun serialize(encoder: Encoder, value: BlockData) {
        BlockStateSerializer.serialize(encoder, value.nmsBlockState)
    }
    
    override fun deserialize(decoder: Decoder): BlockData {
        return BlockStateSerializer.deserialize(decoder).bukkitBlockData
    }
    
}