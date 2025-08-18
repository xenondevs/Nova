package xyz.xenondevs.nova.world.generation.wrapper

import com.mojang.serialization.Decoder
import com.mojang.serialization.Encoder
import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.toResourceLocation
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@Suppress("UNCHECKED_CAST")
@ExperimentalWorldGen
class WrapperBlock(novaBlock: NovaBlock) : Block(
    Properties.of().setId(ResourceKey.create(NovaRegistries.WRAPPER_BLOCK.key(), novaBlock.id.toResourceLocation()) as ResourceKey<Block>)
) {
    
    init {
        this.defaultBlockState = WrapperBlockState(novaBlock.defaultBlockState)
    }
    
}

class WrapperBlockState(val novaState: NovaBlockState) : BlockState(Blocks.STONE, Reference2ObjectArrayMap(), MapCodec.of(Encoder.empty(), Decoder.unit { null }))