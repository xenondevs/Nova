@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.sound

import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.forcePacketBroadcast
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.novaSoundGroup
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine
import kotlin.math.floor
import kotlin.random.Random
import net.minecraft.core.BlockPos as MojangBlockPos
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

private val PLAYER_PLAY_STEP_SOUND_METHOD = ReflectionUtils.getMethod(MojangPlayer::class, true, "playStepSound", MojangBlockPos::class, BlockState::class)
private val ENTITY_WATER_SWIM_SOUND_METHOD = ReflectionUtils.getMethod(MojangEntity::class, true, "waterSwimSound")

internal object SoundPatches : MultiTransformer(MojangPlayer::class, MojangLivingEntity::class, MojangBlock::class, MojangStack::class) {
    
    override fun transform() {
        transformPlayerPlayStepSound()
        transformLivingEntityPlayBlockFallSound()
        transformBlockPlayerWillDestroy()
    }
    
    private fun transformPlayerPlayStepSound() {
        VirtualClassPath[PLAYER_PLAY_STEP_SOUND_METHOD].apply {
            localVariables.clear()
            instructions = buildInsnList {
                aLoad(0)
                aLoad(1)
                aLoad(2)
                invokeStatic(::playerPlayStepSound)
                _return()
            }
        }
    }
    
    @JvmStatic
    fun playerPlayStepSound(player: MojangPlayer, pos: MojangBlockPos, state: BlockState) {
        val level = player.level()
        val world = level.world
        
        if (player.isInWater) {
            ENTITY_WATER_SWIM_SOUND_METHOD.invoke(player)
            playMuffledStepSound(player, pos.toNovaPos(world), state)
        } else {
            val primaryPos = getPrimaryStepSoundBlockPos(player, pos)
            if (primaryPos != pos) {
                val primaryState = level.getBlockState(primaryPos)
                if (primaryState.`is`(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                    playStepSound(player, primaryPos.toNovaPos(world), primaryState)
                    playMuffledStepSound(player, pos.toNovaPos(world), state)
                } else playStepSound(player, primaryPos.toNovaPos(world), primaryState)
            } else playStepSound(player, pos.toNovaPos(world), state)
        }
    }
    
    private fun getPrimaryStepSoundBlockPos(player: MojangPlayer, pos: MojangBlockPos): MojangBlockPos {
        val above = pos.above()
        val state = player.level().getBlockState(above)
        
        if (!state.`is`(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !state.`is`(BlockTags.COMBINATION_STEP_SOUND_BLOCKS))
            return pos
        
        return above
    }
    
    private fun playStepSound(player: MojangPlayer, pos: BlockPos, state: BlockState) {
        playStepSound(player, pos, state, 0.15f, 1f)
    }
    
    private fun playMuffledStepSound(player: MojangPlayer, pos: BlockPos, state: BlockState) {
        playStepSound(player, pos, state, 0.05f, 0.8f)
    }
    
    private fun playStepSound(player: MojangPlayer, pos: BlockPos, state: BlockState, volumeMultiplier: Float, pitchMultiplier: Float) {
        val novaState = WorldDataManager.getBlockState(pos)
        
        val oldSound: String
        val newSound: String
        val volume: Float
        val pitch: Float
        
        val vanillaSoundType = state.soundType
        if (novaState != null) {
            val soundGroup = novaState.block.getBehaviorOrNull<BlockSounds>()?.soundGroup ?: return
            oldSound = vanillaSoundType.stepSound.location.name
            newSound = soundGroup.stepSound
            volume = soundGroup.volume * volumeMultiplier
            pitch = soundGroup.pitch * pitchMultiplier
        } else {
            oldSound = vanillaSoundType.stepSound.location.name
            newSound = oldSound
            volume = vanillaSoundType.volume * volumeMultiplier
            pitch = vanillaSoundType.pitch * pitchMultiplier
        }
        
        playSound(player, oldSound, newSound, volume, pitch)
    }
    
    private fun transformLivingEntityPlayBlockFallSound() {
        VirtualClassPath[ReflectionRegistry.LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD]
            .instructions = buildInsnList {
            aLoad(0)
            invokeStatic(::playFallSound)
            _return()
        }
    }
    
    @JvmStatic
    fun playFallSound(entity: MojangLivingEntity) {
        if (!entity.isSilent) {
            val x = floor(entity.x).toInt()
            val y = floor(entity.y).toInt()
            val z = floor(entity.z).toInt()
            
            val pos = BlockPos(entity.level().world, x, y, z)
            val block = pos.block
            
            if (!block.type.isAir) {
                val soundGroup = block.novaSoundGroup ?: return
                val newSound = soundGroup.fallSound
                val oldSound = block.type.soundGroup.fallSound.key.key
                
                playSound(entity, oldSound, newSound, soundGroup.fallVolume, soundGroup.fallPitch)
            }
        }
    }
    
    @JvmStatic
    fun playSound(entity: MojangEntity, oldSound: String, newSound: String, volume: Float, pitch: Float) {
        val level = entity.level()
        val player = if (SoundEngine.overridesSound(oldSound)) null else entity as? MojangPlayer
        
        val pos = entity.position()
        val packet = ClientboundSoundPacket(
            Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation(newSound))),
            entity.soundSource,
            pos.x,
            pos.y,
            pos.z,
            volume, pitch,
            level.random.nextLong()
        )
        
        MINECRAFT_SERVER.playerList.broadcast(player, entity.x, entity.y, entity.z, 16.0, level.dimension(), packet)
    }
    
    private fun transformBlockPlayerWillDestroy() {
        VirtualClassPath[Block::playerWillDestroy].instructions.replaceFirst(0, 0, buildInsnList {
            aLoad(1)
            aLoad(2)
            invokeStatic(::playBreakSound)
            areturn()
        }) { it.opcode == Opcodes.ARETURN }
    }
    
    @JvmStatic
    fun playBreakSound(level: Level, pos: MojangBlockPos) {
        val novaPos = pos.toNovaPos(level.world)
        val soundGroup = novaPos.block.novaSoundGroup ?: return
        val oldSound = novaPos.block.type.soundGroup.breakSound.key.key
        
        // send custom break sound if it's overridden
        if (SoundEngine.overridesSound(oldSound)) {
            val pitch = soundGroup.breakPitch
            val volume = soundGroup.breakVolume
            MINECRAFT_SERVER.playerList.broadcast(
                null,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                if (volume > 1.0) 16.0 * volume else 16.0,
                level.dimension(),
                ClientboundSoundPacket(
                    Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation(soundGroup.breakSound))),
                    SoundSource.BLOCKS,
                    pos.x + 0.5,
                    pos.y + 0.5,
                    pos.z + 0.5,
                    volume, pitch,
                    Random.nextLong()
                )
            )
        }
    }
    
    @JvmStatic
    fun getEquipSound(itemStack: MojangStack): SoundEvent? {
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            val soundEventName = novaItem.getBehaviorOrNull(Wearable::class)?.equipSound
                ?: return null
            return SoundEvent.createVariableRangeEvent(ResourceLocation(soundEventName))
        }
        
        return (itemStack.item as? ArmorItem)?.material?.equipSound
    }
    
    @JvmStatic
    fun playEquipSound(entity: LivingEntity, itemStack: MojangStack) {
        if (itemStack.isEmpty || entity.isSpectator)
            return
        
        val equipSound = (itemStack.item as? ArmorItem)?.material?.equipSound
            ?: return
        
        forcePacketBroadcast { entity.playSound(equipSound, 1f, 1f) }
    }
    
}