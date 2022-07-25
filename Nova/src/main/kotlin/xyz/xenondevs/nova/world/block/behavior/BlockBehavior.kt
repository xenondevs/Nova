package xyz.xenondevs.nova.world.block.behavior

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.event.Listener
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.util.setBlockStateSilently
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.model.BlockStateBlockModelProvider
import java.util.function.Predicate

internal abstract class BlockBehavior(defaultStateConfig: BlockStateConfig, val runUpdateLater: Boolean) : Listener {
    
    val defaultState = defaultStateConfig.blockState
    private val block = defaultState.block
    
    val blockStatePredicate = Predicate<BlockState> { it.block == block}
    
    open fun init() = Unit
    
    fun handleQueryResult(positions: List<BlockPos>) {
        positions.forEach { it.setBlockStateSilently(defaultState) }
    }
    
    fun getCorrectBlockState(pos: BlockPos): BlockState? {
        var state = WorldDataManager.getBlockState(pos)
        
        if (state is LinkedBlockState)
            state = state.blockState
        
        if (state is NovaBlockState)
            return (state.modelProvider as? BlockStateBlockModelProvider)?.currentBlockState
        
        return defaultState
    }
    
}