package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

internal object ApiBlockManager : IBlockManager {
    
    override fun hasBlock(location: Location): Boolean {
        return BlockManager.hasBlock(location.pos, true)
    }
    
    override fun getBlock(location: Location): NovaBlockState? {
        val state = BlockManager.getBlock(location.pos, true) ?: return null
        if (state is NovaTileEntityState)
            return ApiNovaTileEntityStateWrapper(state)
        return ApiNovaBlockStateWrapper(state)
    }
    
    override fun placeBlock(location: Location, material: INovaBlock, source: Any?, playSound: Boolean) {
        require(material is ApiBlockWrapper) { "material must be ApiBlockWrapper" }
        val ctx = BlockPlaceContext.forAPI(location, material, source)
        BlockManager.placeBlock(material.block, ctx, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctx = BlockBreakContext.forAPI(location, source, tool)
        return BlockManager.getDrops(ctx)
    }
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctx = BlockBreakContext.forAPI(location, source, null)
        return BlockManager.removeBlock(ctx, breakEffects)
    }
    
}