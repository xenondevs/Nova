package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.soundGroup
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.logic.sound.BlockSoundEngine

internal object BlockSoundPatches : MultiTransformer(setOf(Entity::class), computeFrames = true) {
    
    override fun transform() {
        transformEntityPlayStepSound()
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
    fun playStepSound(entity: Entity, nmsPos: BlockPos, state: BlockState) {
        if (!state.material.isLiquid) {
            val level = entity.level
            val above = level.getBlockState(nmsPos.above())
            val setPos = (if (above.`is`(BlockTags.INSIDE_STEP_SOUND_BLOCKS)) nmsPos.above() else nmsPos).toNovaPos(level.world)
            
            val block = setPos.block
            val soundGroup = block.soundGroup ?: return
            val newSound = soundGroup.stepSound
            val oldSound = block.type.soundGroup.stepSound.key.key
            
            val player = if (BlockSoundEngine.overridesSound(oldSound)) null else entity as? Player
            
            val packet = ClientboundCustomSoundPacket(
                ResourceLocation(newSound),
                entity.soundSource,
                entity.position(),
                soundGroup.volume * 0.15f, soundGroup.pitch,
                level.random.nextLong()
            )
            
            minecraftServer.playerList.broadcast(player, entity.x, entity.y, entity.z, 16.0, level.dimension(), packet)
        }
    }
    
}