package xyz.xenondevs.nova.api

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlockState
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
internal object ApiBlockManager : IBlockManager {
    
    override fun hasBlock(location: Location): Boolean {
        return BlockManager.hasBlockState(location.pos, true)
    }
    
    override fun getBlock(location: Location): NovaBlockState? {
        val state = BlockManager.getBlockState(location.pos, true) ?: return null
        if (state is NovaTileEntityState)
            return ApiNovaTileEntityStateWrapper(state)
        return ApiNovaBlockStateWrapper(state)
    }
    
    override fun placeBlock(location: Location, block: INovaBlock, source: Any?, playSound: Boolean) {
        require(block is ApiBlockWrapper) { "material must be ApiBlockWrapper" }
        val ctx = BlockPlaceContext.forAPI(location, block, source)
        BlockManager.placeBlockState(block.block, ctx, playSound)
    }
    
    override fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean) {
        val block = ApiBlockRegistry.get(material.id)
        placeBlock(location, block, source, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctx = BlockBreakContext.forAPI(location, source, tool)
        return BlockManager.getDrops(ctx)
    }
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctx = BlockBreakContext.forAPI(location, source, null)
        return BlockManager.removeBlockState(ctx, breakEffects)
    }
    
}