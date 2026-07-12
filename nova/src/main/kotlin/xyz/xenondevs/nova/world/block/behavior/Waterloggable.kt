package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.novaBlock
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.WATERLOGGED

/**
 * Enables vanilla waterlogging mechanics for a block.
 */
object Waterloggable : BlockBehavior {
    
    override val stateProperties = setOf(WATERLOGGED)
    
    override fun updateShape(
        block: Block,
        state: NovaBlockState,
        neighbor: Block,
        neighborState: BlockData
    ): NovaBlockState {
        NovaWaterloggingBridge.scheduleWaterTickIfWaterlogged(
            state.nmsBlockState,
            block.world.serverLevel,
            block.nmsPos
        )
        
        if (state[WATERLOGGED] != true || neighbor.x != block.x || neighbor.y != block.y + 1 || neighbor.z != block.z)
            return state
        
        val novaBlock = state.blockType.novaBlock ?: return state
        (novaBlock.modelProviders.get()[state.nmsBlockState] as? DisplayEntityBlockModelProvider)
            ?.updateWaterlogEntity(block)
        
        return state
    }
    
}