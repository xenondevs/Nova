package xyz.xenondevs.nova.world.generation.wrapper

import com.google.common.collect.ImmutableMap
import com.mojang.serialization.Decoder
import com.mojang.serialization.Encoder
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BLOCK_DEFAULT_BLOCK_STATE_FIELD
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

/**
 * Class name is forced because of a check in [Block]
 */
@ExperimentalWorldGen
class WrapperBlock(val novaBlock: NovaBlock): Block(Properties.of()) {
    
    init {
        BLOCK_DEFAULT_BLOCK_STATE_FIELD[this] = WrapperBlockState(novaBlock)
    }
    
}

class WrapperBlockState(val novaBlock: NovaBlock): BlockState(Blocks.STONE, ImmutableMap.of(), MapCodec.of(Encoder.empty(), Decoder.unit { null }))