@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNova
import xyz.xenondevs.nova.world.block.novaTileEntity
import java.util.*
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockState

internal object ApiBlockManager : IBlockManager {
    
    override fun hasBlock(location: Location): Boolean {
        return location.block.blockType.isNova
    }
    
    override fun getBlock(location: Location): INovaBlockState? {
        val state = location.block.blockData as? NovaBlockState
            ?: return null
        
        val tileEntity = location.block.novaTileEntity
        if (tileEntity != null)
            return ApiNovaTileEntityStateWrapper(location.block, state, tileEntity)
        
        return ApiNovaBlockStateWrapper(location.block, state)
    }
    
    override fun placeBlock(location: Location, block: INovaBlock, source: Any?, playSound: Boolean) {
        require(block is ApiBlockWrapper) { "block must be ApiBlockWrapper" }
        val targetBlock = location.block
        
        val ctxBuilder = Context.intention(BlockPlace)
            .param(BlockPlace.BLOCK, targetBlock)
            .param(BlockPlace.BLOCK_TYPE, block.block.entry.get())
            .param(BlockPlace.PREVIOUS_BLOCK_STATE, targetBlock.blockData)
            .param(BlockPlace.BLOCK_PLACE_EFFECTS, playSound)
        when (source) {
            is Entity -> ctxBuilder.param(BlockPlace.SOURCE_ENTITY, source)
            is ApiTileEntityWrapper -> ctxBuilder.param(BlockPlace.SOURCE_TILE_ENTITY, source.tileEntity)
            is Location -> ctxBuilder.param(BlockPlace.SOURCE_LOCATION, source)
            is UUID -> ctxBuilder.param(BlockPlace.SOURCE_UUID, source)
        }
        BlockUtils.placeBlock(ctxBuilder.build())
    }
    
    override fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean) {
        val block = ApiBlockRegistry.get(material.id)
        placeBlock(location, block, source, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(BlockBreak.BLOCK, location.block)
            .param(BlockBreak.TOOL_ITEM_STACK, tool)
        when (source) {
            is Entity -> ctxBuilder.param(BlockBreak.SOURCE_ENTITY, source)
            is ApiTileEntityWrapper -> ctxBuilder.param(BlockBreak.SOURCE_TILE_ENTITY, source.tileEntity)
            is Location -> ctxBuilder.param(BlockBreak.SOURCE_LOCATION, source)
            is UUID -> ctxBuilder.param(BlockBreak.SOURCE_UUID, source)
        }
        return BlockUtils.getDrops(ctxBuilder.build())
    }
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctxBuilder = Context.intention(BlockBreak)
            .param(BlockBreak.BLOCK, location.block)
            .param(BlockBreak.BLOCK_BREAK_EFFECTS, breakEffects)
        when (source) {
            is Entity -> ctxBuilder.param(BlockBreak.SOURCE_ENTITY, source)
            is ApiTileEntityWrapper -> ctxBuilder.param(BlockBreak.SOURCE_TILE_ENTITY, source.tileEntity)
            is Location -> ctxBuilder.param(BlockBreak.SOURCE_LOCATION, source)
            is UUID -> ctxBuilder.param(BlockBreak.SOURCE_UUID, source)
        }
        BlockUtils.breakBlock(ctxBuilder.build())
        return true
        
    }
    
}
