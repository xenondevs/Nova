package xyz.xenondevs.nova.world.generation.inject.codec

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BLOCK_STATE_CODEC_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.STATE_HOLDER_CODEC_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.util.function.Function

internal object BlockStateCodecOverride : CodecOverride {
    
    private val BLOCK_REGISTRY = Registry.BLOCK
    
    override fun replace() {
        val blockCodec = ResourceLocation.CODEC.flatXmap(::getBlockFromLocation, ::getLocationFromBlock)
        val legacyBlockCodec = ExtraCodecs.idResolverCodec(::getLegacyIdFromBlock, BLOCK_REGISTRY::byId, -1)
        val registryCodec = ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(blockCodec, legacyBlockCodec), BLOCK_REGISTRY::lifecycle) { BLOCK_REGISTRY.lifecycle() }
        val newCodec = (STATE_HOLDER_CODEC_METHOD(null, registryCodec, Function<Block, BlockState>(Block::defaultBlockState)) as Codec<*>).stable()
        ReflectionUtils.setStaticFinalField(BLOCK_STATE_CODEC_FIELD, newCodec)
    }
    
    private fun getBlockFromLocation(rl: ResourceLocation): DataResult<Block> {
        if (rl.namespace != "minecraft")
            TODO()
        return DataResult.success(BLOCK_REGISTRY[rl]) // the block registry is a DefaultedRegistry, so a null check is not necessary
    }
    
    private fun getLocationFromBlock(block: Block) =
        BLOCK_REGISTRY.getResourceKey(block)
            .map { DataResult.success(it.location()) }
            .orElseGet { DataResult.error("Block $block is not registered!") }
    
    private fun getLegacyIdFromBlock(block: Block) =
        if (BLOCK_REGISTRY.getResourceKey(block).isPresent) BLOCK_REGISTRY.getId(block) else -1
    
}