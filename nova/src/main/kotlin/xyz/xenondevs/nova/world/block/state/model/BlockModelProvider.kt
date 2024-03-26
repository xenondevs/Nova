package xyz.xenondevs.nova.world.block.state.model

import xyz.xenondevs.nova.world.BlockPos

/**
 * A block model provider is responsible for showing custom block models to players and placing their colliders.
 * 
 * There should be one instance of this interface per provider type.
 */
internal sealed interface BlockModelProvider<I> {
    
    /**
     * Called when a block that can be displayed using [info] has been loaded at [pos].
     */
    fun load(pos: BlockPos, info: I)
    
    /**
     * Called when a block that can be displayed using [info] has been placed at [pos].
     */
    fun set(pos: BlockPos, info: I)
    
    /**
     * Called when a block that has been displayed at [pos] using this [BlockModelProvider] was destroyed.
     */
    fun remove(pos: BlockPos)
    
    /**
     * Called when a block that has been displayed at [pos] using this [BlockModelProvider] was unloaded.
     */
    fun unload(pos: BlockPos)
    
    /**
     * Called when a block that has been displayed at [pos] using this [BlockModelProvider] was replaced.
     */
    fun replace(pos: BlockPos, prevInfo: I, newInfo: I)
    
}

/**
 * A combination of [BlockModelProvider][provider] and the [information][info] required to display a certain block model with it.
 */
internal data class LinkedBlockModelProvider<I>(
    val provider: BlockModelProvider<I>,
    val info: I
) {
    
    fun load(pos: BlockPos) = provider.load(pos, info)
    
    fun set(pos: BlockPos) = provider.set(pos, info)
    
    fun remove(pos: BlockPos) = provider.remove(pos)
    
    fun unload(pos: BlockPos) = provider.unload(pos)
    
    fun replace(pos: BlockPos, prevInfo: I) = provider.replace(pos, prevInfo, info)
    
    @Suppress("UNCHECKED_CAST")
    fun replace(pos: BlockPos, prevProvider: LinkedBlockModelProvider<*>) {
        if (prevProvider.provider == provider) {
            provider.replace(pos, prevProvider.info as I, info)
        } else {
            prevProvider.remove(pos)
            set(pos)
        }
    }
    
}