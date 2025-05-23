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
private val LAST_HURT_BY_PLAYER_MEMORY_TIME = ReflectionUtils.getField(LivingEntity::class, "lastHurtByPlayerMemoryTime")

/**
 * Prevents fake players from being stored in the lastHurtByPlayer field.
 */
internal object FakePlayerLastHurtPatch : MultiTransformer(LivingEntity::class) {
    
    override fun transform() {
        transformSetLastHurtByPlayer()
    }
    
    /**
     * Inserts at top of `setLastHurtByPlayer(Player, int)`:
     * ```java
     * if (player instanceof FakePlayer) {
     *   this.lastHurtByPlayerMemoryTime = i; // required for experience orbs to be spawned
     *   return;
     * }
     * ```
     */
    private fun transformSetLastHurtByPlayer() {
        val methodNode = VirtualClassPath[SET_LAST_HURT_BY_PLAYER]
        methodNode.localVariables.clear()
        methodNode.instructions.insert(buildInsnList {
            val continueLabel = methodNode.instructions.first as LabelNode
            
            addLabel()
            aLoad(1)
            instanceOf(FakePlayer::class)
            ifeq(continueLabel)
            addLabel()
            aLoad(0)
            aLoad(2)
            putField(LAST_HURT_BY_PLAYER_MEMORY_TIME)
            _return()
        })
    }
    
}