package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.Serializable
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.Block

/**
 * A block model provider is responsible for showing custom block models to players.
 */
@Serializable
internal sealed interface BlockModelProvider {
    
    /**
     * The vanilla block state sent to clients and used as the server-side backing state for this model.
     */
    val clientsideBlockState: BlockState
    
    /**
     * Loads the model for [block], creating any required external representation.
     */
    fun load(block: Block) = Unit
    
    /**
     * Unloads the model for [block], removing any external representation created by [load].
     */
    fun unload(block: Block) = Unit
    
    /**
     * Replaces the model previously managed by [previous] at [block] with this model.
     *
     * By default, this unloads [previous] and then loads this model.
     */
    fun replace(block: Block, previous: BlockModelProvider) {
        previous.unload(block)
        load(block)
    }
    
}
