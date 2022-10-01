package xyz.xenondevs.nova.world.generation.inject.codec.blockstate

import com.mojang.serialization.Codec
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BLOCK_STATE_CODEC_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.STATE_HOLDER_CODEC_METHOD
import xyz.xenondevs.nova.world.generation.inject.codec.CodecOverride
import java.util.function.Function

internal object BlockStateCodecOverride : CodecOverride() {
    
    override fun replace() {
        replace(BLOCK_STATE_CODEC_FIELD, (STATE_HOLDER_CODEC_METHOD(
            null,
            NovaBlockCodec(Registry.BLOCK.byNameCodec()),
            Function(Block::defaultBlockState)
        ) as Codec<*>).stable())
    }
    
}