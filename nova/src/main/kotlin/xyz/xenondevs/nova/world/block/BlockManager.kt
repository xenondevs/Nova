package xyz.xenondevs.nova.world.block

import net.minecraft.core.Holder
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.LinkedBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.util.getBreakParticlesPacket
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.item.hasNoBreakParticles
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker
import xyz.xenondevs.nova.world.block.logic.`break`.BlockBreaking
import xyz.xenondevs.nova.world.block.logic.interact.BlockInteracting
import xyz.xenondevs.nova.world.block.logic.place.BlockPlacing
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class, WorldDataManager::class]
)
object BlockManager {
    
    @InitFun
    private fun init() {
        BlockPlacing.init()
        BlockBreaking.init()
        BlockInteracting.init()
        SoundEngine.init()
    }
    
    fun getBlockState(pos: BlockPos, useLinkedStates: Boolean = true): NovaBlockState? {
        val blockState = WorldDataManager.getBlockState(pos)
        
        if (blockState is NovaBlockState)
            return blockState
        
        if (useLinkedStates && blockState is LinkedBlockState)
            return blockState.blockState
        
        return null
    }
    
    fun hasBlockState(pos: BlockPos, useLinkedStates: Boolean = true): Boolean {
        return getBlockState(pos, useLinkedStates) != null
    }
    
    fun placeBlockState(material: NovaBlock, ctx: Context<BlockPlace>) {
        val state = material.createNewBlockState(ctx)
        WorldDataManager.setBlockState(ctx[ContextParamTypes.BLOCK_POS]!!, state)
        state.handleInitialized(true)
        
        material.logic.handlePlace(state, ctx)
        
        if (ctx[ContextParamTypes.BLOCK_PLACE_EFFECTS])
            playPlaceSound(state, ctx)
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockPlace(state.block, ctx)
    }
    
    fun removeBlockState(ctx: Context<BlockBreak>): Boolean =
        removeBlockStateInternal(ctx, true)
    
    internal fun removeBlockStateInternal(ctx: Context<BlockBreak>, sendEffectsToBreaker: Boolean): Boolean {
        val pos: BlockPos = ctx[ContextParamTypes.BLOCK_POS]!!
        val state = getBlockState(pos) ?: return false
        
        if (state is NovaTileEntityState)
            TileEntityTracker.handleBlockBreak(state.tileEntity, ctx)
        
        if (ctx[ContextParamTypes.BLOCK_BREAK_EFFECTS]) {
            playBreakEffects(state, ctx, pos, sendEffectsToBreaker)
        }
        
        val material = state.block
        material.logic.handleBreak(state, ctx)
        
        WorldDataManager.removeBlockState(state.pos)
        state.handleRemoved(true)
        
        return true
    }
    
    internal fun removeLinkedBlockState(ctx: Context<BlockBreak>, breakEffects: Boolean): Boolean {
        val pos: BlockPos = ctx[ContextParamTypes.BLOCK_POS]!!
        val state = WorldDataManager.getBlockState(pos, takeUnloaded = true) as? LinkedBlockState
            ?: return false
        
        if (breakEffects) {
            playBreakEffects(state.blockState, ctx, pos, true)
        }
        
        WorldDataManager.removeBlockState(pos)
        state.handleRemoved(true)
        
        return true
    }
    
    fun getDrops(ctx: Context<BlockBreak>): List<ItemStack>? {
        val state = getBlockState(ctx[ContextParamTypes.BLOCK_POS]!!) ?: return null
        return state.block.logic.getDrops(state, ctx)
    }
    
    fun breakBlockState(ctx: Context<BlockBreak>): Boolean {
        if (!removeBlockState(ctx)) return false
        val pos: BlockPos = ctx[ContextParamTypes.BLOCK_POS]!!
        getDrops(ctx)?.let { pos.location.add(0.5, 0.5, 0.5).dropItems(it) }
        
        return true
    }
    
    private fun playBreakEffects(state: NovaBlockState, ctx: Context<BlockBreak>, pos: BlockPos, sendEffectsToBreaker: Boolean) {
        val player = ctx[ContextParamTypes.SOURCE_ENTITY] as? Player
        val material = state.block
        val level = pos.world.serverLevel
        val dimension = level.dimension()
        val nmsPos = pos.nmsPos
        
        fun broadcast(packet: Packet<*>, sendEffectsToBreaker: Boolean) {
            MINECRAFT_SERVER.playerList.broadcast(
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
        
        val soundGroup = state.block.options.soundGroup
        if (material.model is BlockStateBlockModelData) {
            // use the level event packet for blocks that use block states
            val levelEventPacket = ClientboundLevelEventPacket(2001, nmsPos, pos.nmsBlockState.id, false)
            broadcast(levelEventPacket, sendEffectsToBreaker)
            
            if (soundGroup != null && SoundEngine.overridesSound(pos.block.type.soundGroup.breakSound.key.key)) {
                broadcastBreakSound(soundGroup)
            }
        } else {
            // send sound and break particles manually for armor stand blocks
            if (soundGroup != null) broadcastBreakSound(soundGroup)
            val breakParticles = state.block.options.breakParticles?.getBreakParticlesPacket(pos.location)
            if (breakParticles != null) broadcast(breakParticles, sendEffectsToBreaker || pos.block.type.hasNoBreakParticles())
        }
    }
    
    private fun playPlaceSound(state: NovaBlockState, ctx: Context<BlockPlace>) {
        val soundGroup = state.block.options.soundGroup
        if (soundGroup != null) {
            ctx[ContextParamTypes.BLOCK_POS]!!.playSound(soundGroup.placeSound, soundGroup.placeVolume, soundGroup.placePitch)
        }
    }
    
}