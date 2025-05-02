package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.Serializable
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
@Serializable
internal sealed interface BlockModelProvider {
    
    /**
     * Places the model at [pos].
     */
    fun set(pos: BlockPos, method: BlockUpdateMethod= BlockUpdateMethod.DEFAULT)
    
    /**
     * Removes the model at [pos].
     */
    fun remove(pos: BlockPos, method: BlockUpdateMethod= BlockUpdateMethod.DEFAULT) {
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
     * Reactivates the model at [pos], assuming the model was already [set] previously.
     */
    fun load(pos: BlockPos)
    
    /**
     * Deactivates the model at [pos].
     */
    fun unload(pos: BlockPos)
    
    /**
     * Replaces the model at [pos].
     */
    fun replace(pos: BlockPos, method: BlockUpdateMethod = BlockUpdateMethod.DEFAULT)
    
    /**
     * Replaces the model at [pos], assuming that the previous model was displayed using [prevProvider].
     */
    fun replace(pos: BlockPos, prevProvider: BlockModelProvider, method: BlockUpdateMethod = BlockUpdateMethod.DEFAULT) {
        if (prevProvider::class == this::class) {
            replace(pos, method)
        } else {
            prevProvider.remove(pos, method)
            set(pos, method)
        }
    }
    
}