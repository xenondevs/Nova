package xyz.xenondevs.nova.world.block.behavior

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.event.Listener
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.model.BlockStateBlockModelProvider

internal abstract class BlockBehavior(defaultStateConfig: BlockStateConfig, val runUpdateLater: Boolean) : Listener {
    
    val defaultState = defaultStateConfig.blockState
    
    open fun init() = Unit
    
    fun getCorrectBlockState(pos: BlockPos): BlockState? {
        var state = WorldDataManager.getBlockState(pos)
        
        if (state is LinkedBlockState)
            state = state.blockState
        
        if (state is NovaBlockState)
            return (state.modelProvider as? BlockStateBlockModelProvider)?.currentBlockState
        
        return defaultState
    }
    
}