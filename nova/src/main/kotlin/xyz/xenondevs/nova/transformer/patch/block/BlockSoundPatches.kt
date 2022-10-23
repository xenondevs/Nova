@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.player.Player
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.soundGroup
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.logic.sound.BlockSoundEngine
import kotlin.math.floor
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.level.block.state.BlockState as MojangBlockState

internal object BlockSoundPatches : MultiTransformer(setOf(MojangEntity::class, MojangLivingEntity::class), computeFrames = true) {
    
    override fun transform() {
        transformEntityPlayStepSound()
        transformLivingEntityPlayBlockFallSound()
    }
    
    private fun transformEntityPlayStepSound() {
        VirtualClassPath[ReflectionRegistry.ENTITY_PLAY_STEP_SOUND_METHOD]
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            invokeStatic(ReflectionUtils.getMethodByName(BlockSoundPatches::class.java, false, "playStepSound"))
            _return()
        }
    }
    
    @JvmStatic
    fun playStepSound(entity: MojangEntity, nmsPos: MojangBlockPos, state: MojangBlockState) {
        if (!state.material.isLiquid) {
            val level = entity.level
            val above = level.getBlockState(nmsPos.above())
            val setPos = (if (above.`is`(BlockTags.INSIDE_STEP_SOUND_BLOCKS)) nmsPos.above() else nmsPos).toNovaPos(level.world)
            
            val block = setPos.block
            val soundGroup = block.soundGroup ?: return
            val newSound = soundGroup.stepSound
            val oldSound = block.type.soundGroup.stepSound.key.key
            
            playSound(entity, oldSound, newSound, soundGroup.volume * .15f, soundGroup.pitch)
        }
    }
    
    private fun transformLivingEntityPlayBlockFallSound() {
        VirtualClassPath[ReflectionRegistry.LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD]
            .instructions = buildInsnList {
            aLoad(0)
            invokeStatic(ReflectionUtils.getMethodByName(BlockSoundPatches::class.java, false, "playFallSound"))
            _return()
        }
    }
    
    @JvmStatic
    fun playFallSound(entity: MojangLivingEntity) {
        if (!entity.isSilent) {
            val x = floor(entity.x).toInt()
            val y = floor(entity.y).toInt()
            val z = floor(entity.z).toInt()
            
            val pos = BlockPos(entity.level.world, x, y, z)
            val block = pos.block
            
            if (!block.type.isAir) {
                val soundGroup = block.soundGroup ?: return
                val newSound = soundGroup.fallSound
                val oldSound = block.type.soundGroup.fallSound.key.key
                
                playSound(entity, oldSound, newSound, soundGroup.volume * .5f, soundGroup.pitch * .75f)
            }
        }
    }
    
    @JvmStatic
    fun playSound(entity: MojangEntity, oldSound: String, newSound: String, volume: Float, pitch: Float) {
        val level = entity.level
        val player = if (BlockSoundEngine.overridesSound(oldSound)) null else entity as? Player
        
        val packet = ClientboundCustomSoundPacket(
            ResourceLocation(newSound),
            entity.soundSource,
            entity.position(),
            volume, pitch,
            level.random.nextLong()
        )
        
        minecraftServer.playerList.broadcast(player, entity.x, entity.y, entity.z, 16.0, level.dimension(), packet)
    }
    
}