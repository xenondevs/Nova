@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlockState
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.context.intention.HasOptionalSource
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.format.WorldDataManager
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
            .param(BlockPlace.BLOCK_POS, location.pos)
            .param(BlockPlace.BLOCK_TYPE_NOVA, block.block)
            .param(BlockPlace.BLOCK_PLACE_EFFECTS, playSound)
        setSourceParam(ctxBuilder, source)
        BlockUtils.placeBlock(ctxBuilder.build())
    }
    
    override fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean) {
        val block = ApiBlockRegistry.get(material.id)
        placeBlock(location, block, source, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(BlockBreak.BLOCK_POS, location.pos)
            .param(BlockBreak.TOOL_ITEM_STACK, tool)
        setSourceParam(ctxBuilder, source)
        return BlockUtils.getDrops(ctxBuilder.build())
    }
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(BlockBreak.BLOCK_POS, location.pos)
            .param(BlockBreak.BLOCK_BREAK_EFFECTS, breakEffects)
        setSourceParam(ctxBuilder, source)
        BlockUtils.breakBlock(ctxBuilder.build())
        return true
        
    }
    
    private fun <I : HasOptionalSource<I>> setSourceParam(builder: Context.Builder<I>, source: Any?) {
        if (source == null)
            return
        
        when (source) {
            is Entity -> builder.param(HasOptionalSource.sourceEntity(), source)
            is ApiTileEntityWrapper -> builder.param(HasOptionalSource.sourceTileEntity(), source.tileEntity)
            is Location -> builder.param(HasOptionalSource.sourceLocation(), source)
            is UUID -> builder.param(HasOptionalSource.sourceUuid(), source)
        }
    }
    
}