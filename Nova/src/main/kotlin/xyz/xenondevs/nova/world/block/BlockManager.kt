package xyz.xenondevs.nova.world.block

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker

object BlockManager : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer, WorldDataManager)
    
    override fun init() {
        LOGGER.info("Initializing BlockManager")
        
        BlockPlacing.init()
        BlockBreaking.init()
        BlockInteracting.init()
    }
    
    fun getBlock(pos: BlockPos, useLinkedStates: Boolean = true): NovaBlockState? {
        val blockState = WorldDataManager.getBlockState(pos)
        
        if (blockState is NovaBlockState)
            return blockState
        
        if (useLinkedStates && blockState is LinkedBlockState && blockState.blockState is NovaBlockState)
            return blockState.blockState
        
        return null
    }
    
    fun placeBlock(material: BlockNovaMaterial, ctx: BlockPlaceContext, playEffects: Boolean = true) {
        val state = material.createNewBlockState(ctx)
        WorldDataManager.setBlockState(ctx.pos, state)
        state.handleInitialized(true)
        
        material.novaBlock.handlePlace(state, ctx)
        if (playEffects)
            material.placeSound?.play(ctx.pos)
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockPlace(state.material, ctx)
    }
    
    fun removeBlock(ctx: BlockBreakContext, playEffects: Boolean = true): Boolean {
        val pos = ctx.pos
        val state = getBlock(ctx.pos) ?: return false
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockBreak(state.tileEntity, ctx)
        
        val material = state.material
        material.novaBlock.handleBreak(state, ctx)
        
        WorldDataManager.removeBlockState(pos)
        state.handleRemoved(true)
        
        if (playEffects && GlobalValues.BLOCK_BREAK_EFFECTS)
            material.novaBlock.playBreakEffects(state, ctx)
        
        return true
    }
    
    fun getDrops(ctx: BlockBreakContext): List<ItemStack>? {
        val state = getBlock(ctx.pos) ?: return null
        return state.material.novaBlock.getDrops(state, ctx)
    }
    
    fun breakBlock(ctx: BlockBreakContext, playEffects: Boolean = true): Boolean {
        if (!removeBlock(ctx, playEffects)) return false
        getDrops(ctx)?.let { ctx.pos.location.add(0.5, 0.5, 0.5).dropItems(it) }
        
        return true
    }
    
}