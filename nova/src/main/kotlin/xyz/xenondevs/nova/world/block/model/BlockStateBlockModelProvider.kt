package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.getBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate

private val AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState()

class BlockStateBlockModelProvider(val blockState: NovaBlockState) : BlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.block
    private val modelData = material.model as BlockStateBlockModelData
    
    override var currentSubId = 0
        private set
    var currentBlockState = getBlockState(0)
        private set
    
    override fun load(placed: Boolean) {
        if (placed || pos.getBlockState() != currentBlockState) update(0)
    }
    
    override fun remove(broken: Boolean) {
        if (broken) pos.setBlockStateNoUpdate(AIR_BLOCK_STATE)
    }
    
    override fun update(subId: Int) {
        if (currentSubId != subId) {
            currentSubId = subId
            currentBlockState = getBlockState(subId)
        }
        
        pos.setBlockStateNoUpdate(currentBlockState)
    }
    
    private fun getBlockState(subId: Int): BlockState {
        val directional = blockState.getProperty(Directional::class)
        val config = if (directional != null)
            modelData[directional.facing, subId]
        else modelData[subId]
        
        return config.blockState
    }
    
    companion object : BlockModelProviderType<BlockStateBlockModelProvider> {
        override fun create(blockState: NovaBlockState) = BlockStateBlockModelProvider(blockState)
    }
    
}