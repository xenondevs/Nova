package xyz.xenondevs.nova.world.block.model

import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

sealed interface BlockModelProvider {
    
    val currentSubId: Int
    
    fun load(placed: Boolean)
    
    fun remove(broken: Boolean)
    
    fun update(subId: Int = 0)
    
}

sealed interface SolidBlockModelProvider : BlockModelProvider {
    
    val currentBlockState: BlockState
    
}

sealed interface BlockModelProviderType<T : BlockModelProvider> {
    
    fun create(blockState: NovaBlockState): T
    
}