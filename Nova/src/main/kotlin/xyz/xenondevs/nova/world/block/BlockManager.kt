package xyz.xenondevs.nova.world.block

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.config.GlobalValues
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.util.playBreakSound
import xyz.xenondevs.nova.util.showBreakParticles
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager

object BlockManager : Initializable(), IBlockManager {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer, WorldDataManager)
    
    override fun init() {
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
    
    fun hasBlock(pos: BlockPos, useLinkedStates: Boolean = true): Boolean {
        return getBlock(pos, useLinkedStates) != null
    }
    
    fun placeBlock(material: BlockNovaMaterial, ctx: BlockPlaceContext, playSound: Boolean = true) {
        val state = material.createNewBlockState(ctx)
        WorldDataManager.setBlockState(ctx.pos, state)
        state.handleInitialized(true)
        
        material.novaBlock.handlePlace(state, ctx)
        if (playSound)
            material.placeSound?.play(ctx.pos)
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockPlace(state.material, ctx)
    }
    
    fun removeBlock(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true): Boolean {
        val state = getBlock(ctx.pos) ?: return false
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockBreak(state.tileEntity, ctx)
        
        val material = state.material
        material.novaBlock.handleBreak(state, ctx)
        
        WorldDataManager.removeBlockState(state.pos)
        state.handleRemoved(true)
        
        if (playSound)
            material.novaBlock.playBreakSound(state, ctx)
        
        if (showParticles && GlobalValues.BLOCK_BREAK_EFFECTS)
            material.novaBlock.showBreakParticles(state, ctx)
        
        return true
    }
    
    internal fun removeLinkedBlock(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true): Boolean {
        val pos = ctx.pos
        val state = WorldDataManager.getBlockState(pos) as? LinkedBlockState ?: return false
        
        WorldDataManager.removeBlockState(pos)
        state.handleRemoved(true)
        
        val mainState = state.blockState
        val novaBlock = (mainState as? NovaBlockState)?.material?.novaBlock
        
        if (playSound) {
            if (novaBlock != null)
                novaBlock.playBreakSound(mainState, ctx)
            else mainState.pos.block.playBreakSound()
        }
        
        if (showParticles && GlobalValues.BLOCK_BREAK_EFFECTS) {
            if (novaBlock != null)
                novaBlock.showBreakParticles(mainState, ctx)
            else mainState.pos.block.showBreakParticles()
        }
        
        return true
    }
    
    fun getDrops(ctx: BlockBreakContext): List<ItemStack>? {
        val state = getBlock(ctx.pos) ?: return null
        return state.material.novaBlock.getDrops(state, ctx)
    }
    
    fun breakBlock(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true): Boolean {
        if (!removeBlock(ctx, playSound, showParticles)) return false
        getDrops(ctx)?.let { ctx.pos.location.add(0.5, 0.5, 0.5).dropItems(it) }
        
        return true
    }
    
    //<editor-fold desc="NovaAPI methods">
    
    override fun hasBlock(location: Location): Boolean {
        return hasBlock(location.pos, true)
    }
    
    override fun getBlock(location: Location): NovaBlockState? {
        return getBlock(location.pos, true)
    }
    
    override fun placeBlock(location: Location, material: NovaMaterial, source: Any?, playSound: Boolean) {
        require(material is BlockNovaMaterial)
        
        val ctx = BlockPlaceContext.forAPI(location, material, source)
        placeBlock(material, ctx, playSound)
    }
    
    override fun getDrops(location: Location, source: Any?, tool: ItemStack?): List<ItemStack>? {
        val ctx = BlockBreakContext.forAPI(location, source, tool)
        return getDrops(ctx)
    }
    
    override fun removeBlock(location: Location, source: Any?, playSound: Boolean, showParticles: Boolean): Boolean {
        val ctx = BlockBreakContext.forAPI(location, source, null)
        return removeBlock(ctx, playSound, showParticles)
    }
    
    //</editor-fold>
    
}