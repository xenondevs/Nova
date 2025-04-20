package xyz.xenondevs.nova.patch.impl.misc

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.FakePlayer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val SET_LAST_HURT_BY_PLAYER = ReflectionUtils.getMethod(LivingEntity::class, "setLastHurtByPlayer", Player::class, Int::class)

/**
 * Patches several methods to prevent fake players from being stored in the lastHurtByPlayer field.
 */
internal object FakePlayerLastHurtPatch : MultiTransformer(LivingEntity::class) {
    
    override fun transform() {
        transformSetLastHurtByPlayer()
    }
    
    private fun transformSetLastHurtByPlayer() {
        val methodNode = VirtualClassPath[SET_LAST_HURT_BY_PLAYER]
        methodNode.localVariables.clear()
        methodNode.instructions.insert(buildInsnList {
            val continueLabel = methodNode.instructions.first as LabelNode
            
            addLabel()
            aLoad(1)
            instanceOf(FakePlayer::class)
            ifne(continueLabel)
            addLabel()
            _return()
        })
    }
    
}