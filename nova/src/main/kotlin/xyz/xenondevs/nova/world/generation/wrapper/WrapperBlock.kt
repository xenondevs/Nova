package xyz.xenondevs.nova.world.generation.wrapper

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BLOCK_DEFAULT_BLOCK_STATE_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.STATE_HOLDER_PROPERTIES_CODEC_FIELD

class WrapperBlock(val delegate: Block): Block(buildProperties(delegate)) {
    
    init {
        BLOCK_DEFAULT_BLOCK_STATE_FIELD[this] = WrapperBlockState(BLOCK_DEFAULT_BLOCK_STATE_FIELD[delegate] as BlockState)
    }
    
    companion object {
        private fun buildProperties(block: Block) =
            Properties.of(block.defaultBlockState().material)
    }
    
}

@Suppress("UNCHECKED_CAST")
class WrapperBlockState(val delegate: BlockState): BlockState(delegate.block, delegate.values, STATE_HOLDER_PROPERTIES_CODEC_FIELD[delegate] as MapCodec<BlockState>?)