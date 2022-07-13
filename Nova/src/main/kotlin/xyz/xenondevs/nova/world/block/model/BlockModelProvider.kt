package xyz.xenondevs.nova.world.block.model

import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

sealed interface BlockModelProvider {
    
    val currentSubId: Int
    
    fun load(placed: Boolean)
    
    fun remove(broken: Boolean)
    
    fun update(subId: Int = 0)
    
}

sealed interface BlockModelProviderType<T : BlockModelProvider> {
    
    fun create(blockState: NovaBlockState): T
    
}