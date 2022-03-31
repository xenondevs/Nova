package xyz.xenondevs.nova.world.block.model

import xyz.xenondevs.nova.data.world.block.state.NovaBlockState

interface BlockModelProvider {
    
    fun load(placed: Boolean)
    
    fun remove(broken: Boolean)
    
    fun update(subId: Int = 0)
    
}

interface BlockModelProviderType<T : BlockModelProvider> {
    
    fun create(blockState: NovaBlockState): T
    
}