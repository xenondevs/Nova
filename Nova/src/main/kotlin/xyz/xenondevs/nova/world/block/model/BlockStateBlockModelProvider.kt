package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.getBlockState
import xyz.xenondevs.nova.util.setBlockStateSilently

class BlockStateBlockModelProvider(val blockState: NovaBlockState) : BlockModelProvider {
    
    private val pos = blockState.pos
    private val material = blockState.material
    private val modelData = material.block as BlockStateBlockModelData
    
    override var currentSubId = 0
        private set
    var currentBlockState = getBlockState(0)
        private set
    
    override fun load(placed: Boolean) {
        if (placed || pos.getBlockState() != currentBlockState) update(0)
    }
    
    override fun remove(broken: Boolean) {
        if (broken) pos.block.type = Material.AIR
    }
    
    override fun update(subId: Int) {
        if (currentSubId != subId) {
            currentSubId = subId
            currentBlockState = getBlockState(subId)
        }
        
        pos.setBlockStateSilently(currentBlockState)
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