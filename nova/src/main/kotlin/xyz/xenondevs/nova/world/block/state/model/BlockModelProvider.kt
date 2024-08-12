package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Blocks
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.setBlockStateNoUpdate
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos

/**
 * Defines how vanilla block states should be set.
 */
internal enum class BlockUpdateMethod {
    
    /**
     * The default way of setting a block, with block updates.
     */
    DEFAULT,
    
    /**
     * Places the block without block updates.
     */
    NO_UPDATE,
    
    /**
     * Places the block without block updates and without notifying the client.
     */
    SILENT
    
}

/**
 * A block model provider is responsible for showing custom block models to players and placing their colliders.
 *
 * There should be one instance of this interface per provider type.
 */
internal sealed interface BlockModelProvider<I> {
    
    /**
     * Places the model [info] at [pos].
     */
    fun set(pos: BlockPos, info: I, method: BlockUpdateMethod)
    
    /**
     * Removes the model at [pos].
     */
    fun remove(pos: BlockPos, method: BlockUpdateMethod) {
        val air = Blocks.AIR.defaultBlockState()
        withoutBlockMigration(pos) {
            when (method) {
                BlockUpdateMethod.DEFAULT -> pos.setBlockState(air)
                BlockUpdateMethod.NO_UPDATE -> pos.setBlockStateNoUpdate(air)
                BlockUpdateMethod.SILENT -> pos.setBlockStateSilently(air)
            }
        }
    }
    
    /**
     * Reactivates the model [info] at [pos], assuming the model
     * was already [set] previously.
     */
    fun load(pos: BlockPos, info: I)
    
    /**
     * Deactivates the model at [pos].
     */
    fun unload(pos: BlockPos)
    
    /**
     * Replaces the model at [pos] with [info].
     */
    fun replace(pos: BlockPos, info: I, method: BlockUpdateMethod)
    
}

/**
 * A combination of [BlockModelProvider][provider] and the [information][info] required to display a certain block model with it.
 */
internal data class LinkedBlockModelProvider<I>(
    val provider: BlockModelProvider<I>,
    val info: I
) {
    
    fun set(pos: BlockPos, method: BlockUpdateMethod = BlockUpdateMethod.DEFAULT) = provider.set(pos, info, method)
    
    fun remove(pos: BlockPos, method: BlockUpdateMethod = BlockUpdateMethod.DEFAULT) = provider.remove(pos, method)
    
    fun load(pos: BlockPos) = provider.load(pos, info)
    
    fun unload(pos: BlockPos) = provider.unload(pos)
    
    fun replace(pos: BlockPos, method: BlockUpdateMethod = BlockUpdateMethod.DEFAULT) = provider.replace(pos, info, method)
    
    fun replace(pos: BlockPos, prevProvider: LinkedBlockModelProvider<*>) {
        if (prevProvider.provider == provider) {
            provider.replace(pos, info, BlockUpdateMethod.DEFAULT)
        } else {
            prevProvider.remove(pos)
            set(pos)
        }
    }
    
}