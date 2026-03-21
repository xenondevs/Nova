package xyz.xenondevs.nova.world.generation.wrapper

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.registry.LegacyNovaRegistries
import xyz.xenondevs.nova.util.toIdentifier
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@Suppress("UNCHECKED_CAST")
@ExperimentalWorldGen
class WrapperBlock(novaBlock: NovaBlock) : Block(
    Properties.of().setId(ResourceKey.create(LegacyNovaRegistries.WRAPPER_BLOCK.key(), novaBlock.key.toIdentifier()) as ResourceKey<Block>)
) {
    
    init {
        this.defaultBlockState = WrapperBlockState(novaBlock.defaultBlockState)
    }
    
}

class WrapperBlockState(val novaState: NovaBlockState) : BlockState(Blocks.STONE, emptyArray(), emptyArray())