package xyz.xenondevs.nova.world.block.state.model

import kotlinx.serialization.Serializable
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.Block

/**
 * A block model provider is responsible for showing custom block models to players and placing their colliders.
 *
 * There should be one instance of this interface per provider type.
 */
@Serializable
internal sealed interface BlockModelProvider {
    
    val clientsideBlockState: BlockState
    
    fun load(block: Block) = Unit
    
    fun unload(block: Block) = Unit
    
    fun replace(block: Block, previous: BlockModelProvider) {
        previous.unload(block)
        load(block)
    }
    
}
