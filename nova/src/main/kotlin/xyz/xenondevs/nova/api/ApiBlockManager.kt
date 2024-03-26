@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlockState
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.pos
import java.util.*
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

internal object ApiBlockManager : IBlockManager {
    
    override fun hasBlock(location: Location): Boolean {
        return WorldDataManager.getBlockState(location.pos) != null
    }
    
    override fun getBlock(location: Location): NovaBlockState? {
        val pos = location.pos
        
        val state = WorldDataManager.getBlockState(pos)
            ?: return null
        
        if (state.block is NovaTileEntityBlock) {
            val tileEntity = WorldDataManager.getTileEntity(pos)
                ?: return null
            return ApiNovaTileEntityStateWrapper(pos, state, tileEntity)
        } else {
            return ApiNovaBlockStateWrapper(pos, state)
        }
    }
    
    override fun placeBlock(location: Location, block: INovaBlock, source: Any?, playSound: Boolean) {
        require(block is ApiBlockWrapper) { "block must be ApiBlockWrapper" }
        
        val ctxBuilder = Context.intention(BlockPlace)
            .param(ContextParamTypes.BLOCK_POS, location.pos)
            .param(ContextParamTypes.BLOCK_TYPE_NOVA, block.block)
            .param(ContextParamTypes.BLOCK_PLACE_EFFECTS, playSound)
        setSourceParam(ctxBuilder, source)
        BlockUtils.placeBlock(ctxBuilder.build())
    }
    
    override fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean) {
        val block = ApiBlockRegistry.get(material.id)
        placeBlock(location, block, source, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(ContextParamTypes.BLOCK_POS, location.pos)
            .param(ContextParamTypes.TOOL_ITEM_STACK, tool)
        setSourceParam(ctxBuilder, source)
         return BlockUtils.getDrops(ctxBuilder.build())
    }
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(ContextParamTypes.BLOCK_POS, location.pos)
            .param(ContextParamTypes.BLOCK_BREAK_EFFECTS, breakEffects)
        setSourceParam(ctxBuilder, source)
        BlockUtils.breakBlock(ctxBuilder.build())
        return true
        
    }
    
    private fun setSourceParam(builder: Context.Builder<*>, source: Any?) {
        if (source == null)
            return
        
        when (source) {
            is Entity -> builder.param(ContextParamTypes.SOURCE_ENTITY, source)
            is ApiTileEntityWrapper -> builder.param(ContextParamTypes.SOURCE_TILE_ENTITY, source.tileEntity)
            is Location -> builder.param(ContextParamTypes.SOURCE_LOCATION, source)
            is UUID -> builder.param(ContextParamTypes.SOURCE_UUID, source)
        }
    }
    
}