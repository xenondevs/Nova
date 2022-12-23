package xyz.xenondevs.nova.world.block

import net.minecraft.core.Holder
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.api.material.NovaMaterial
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.util.getBreakParticlesPacket
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.item.hasNoBreakParticles
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.block.logic.interact.BlockInteracting
import xyz.xenondevs.nova.world.block.logic.place.BlockPlacing
import xyz.xenondevs.nova.world.block.logic.sound.BlockSoundEngine
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.pos
import kotlin.random.Random
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager

object BlockManager : Initializable(), IBlockManager {
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(AddonsInitializer, WorldDataManager)
    
    override fun init() {
        BlockPlacing.init()
        BlockBreaking.init()
        BlockInteracting.init()
        BlockSoundEngine.init()
    }
    
    fun getBlock(pos: BlockPos, useLinkedStates: Boolean = true): NovaBlockState? {
        val blockState = WorldDataManager.getBlockState(pos)
        
        if (blockState is NovaBlockState)
            return blockState
        
        if (useLinkedStates && blockState is LinkedBlockState)
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
            playPlaceSound(state, ctx)
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockPlace(state.material, ctx)
    }
    
    fun removeBlock(ctx: BlockBreakContext, breakEffects: Boolean = true): Boolean =
        removeBlockInternal(ctx, breakEffects, true)
    
    internal fun removeBlockInternal(ctx: BlockBreakContext, breakEffects: Boolean, sendEffectsToBreaker: Boolean): Boolean {
        val pos = ctx.pos
        val state = getBlock(pos) ?: return false
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockBreak(state.tileEntity, ctx)
        
        if (breakEffects) {
            playBreakEffects(state, ctx, pos, sendEffectsToBreaker)
        }
        
        val material = state.material
        material.novaBlock.handleBreak(state, ctx)
        
        WorldDataManager.removeBlockState(state.pos)
        state.handleRemoved(true)
        
        return true
    }
    
    internal fun removeLinkedBlock(ctx: BlockBreakContext, breakEffects: Boolean): Boolean {
        val pos = ctx.pos
        val state = WorldDataManager.getBlockState(pos, takeUnloaded = true) as? LinkedBlockState
            ?: return false
        
        if (breakEffects) {
            playBreakEffects(state.blockState, ctx, pos, true)
        }
        
        WorldDataManager.removeBlockState(pos)
        state.handleRemoved(true)
        
        return true
    }
    
    fun getDrops(ctx: BlockBreakContext): List<ItemStack>? {
        val state = getBlock(ctx.pos) ?: return null
        return state.material.novaBlock.getDrops(state, ctx)
    }
    
    fun breakBlock(ctx: BlockBreakContext, breakEffects: Boolean = true): Boolean {
        if (!removeBlock(ctx, breakEffects)) return false
        getDrops(ctx)?.let { ctx.pos.location.add(0.5, 0.5, 0.5).dropItems(it) }
        
        return true
    }
    
    private fun playBreakEffects(state: NovaBlockState, ctx: BlockBreakContext, pos: BlockPos, sendEffectsToBreaker: Boolean) {
        val player = ctx.source as? Player
        val material = state.material
        val level = pos.world.serverLevel
        val dimension = level.dimension()
        val nmsPos = pos.nmsPos
        
        fun broadcast(packet: Packet<*>, sendEffectsToBreaker: Boolean) {
            minecraftServer.playerList.broadcast(
                if (sendEffectsToBreaker) null else player?.serverPlayer,
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                64.0,
                dimension,
                packet
            )
        }
        
        fun broadcastBreakSound(soundGroup: SoundGroup) {
            val soundPacket = ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation(soundGroup.breakSound))),
                SoundSource.BLOCKS,
                nmsPos.x + 0.5,
                nmsPos.y + 0.5,
                nmsPos.z + 0.5,
                soundGroup.breakVolume,
                soundGroup.breakPitch,
                Random.nextLong()
            )
            
            broadcast(soundPacket, true)
        }
        
        val soundGroup = state.material.soundGroup
        if (material.block is BlockStateBlockModelData) {
            // use the level event packet for blocks that use block states
            val levelEventPacket = ClientboundLevelEventPacket(2001, nmsPos, pos.nmsBlockState.id, false)
            broadcast(levelEventPacket, sendEffectsToBreaker)
            
            if (soundGroup != null && BlockSoundEngine.overridesSound(pos.block.type.soundGroup.breakSound.key.key)) {
                broadcastBreakSound(soundGroup)
            }
        } else {
            // send sound and break particles manually for armor stand blocks
            if (soundGroup != null) broadcastBreakSound(soundGroup)
            val breakParticles = state.material.breakParticles?.getBreakParticlesPacket(pos.location)
            if (breakParticles != null) broadcast(breakParticles, sendEffectsToBreaker || pos.block.type.hasNoBreakParticles())
        }
    }
    
    private fun playPlaceSound(state: NovaBlockState, ctx: BlockPlaceContext) {
        val soundGroup = state.material.soundGroup
        if (soundGroup != null) {
            ctx.pos.playSound(soundGroup.placeSound, soundGroup.placeVolume, soundGroup.placePitch)
        }
    }
    
    //<editor-fold desc="deprecated methods", defaultstate="collapsed">
    @Deprecated("Break sound and particles are not independent from one another", ReplaceWith("removeBlock(ctx, playSound || showParticles)"))
    fun removeBlock(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true): Boolean =
        removeBlock(ctx, playSound || showParticles)
    
    @Deprecated("Break sound and particles are not independent from one another", ReplaceWith("breakBlock(ctx, playSound || showParticles)"))
    fun breakBlock(ctx: BlockBreakContext, playSound: Boolean = true, showParticles: Boolean = true): Boolean =
        breakBlock(ctx, playSound || showParticles)
    //</editor-fold>
    
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
    
    override fun removeBlock(location: Location, source: Any?, breakEffects: Boolean): Boolean {
        val ctx = BlockBreakContext.forAPI(location, source, null)
        return removeBlock(ctx, breakEffects)
    }
    
    //</editor-fold>
    
}