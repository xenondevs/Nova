package xyz.xenondevs.nova.patch.impl.misc

import net.minecraft.world.entity.LivingEntity
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.puts
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.FakePlayer

/**
 * Patches several methods to prevent fake players from being stored in the lastHurtByPlayer field.
 */
internal object FakePlayerLastHurtPatch : MultiTransformer(LivingEntity::class) {
    
    override fun transform() {
        transformSetLastHurtByPlayer()
        transformHurt()
    }
    
    private fun transformSetLastHurtByPlayer() {
        val methodNode = VirtualClassPath[LivingEntity::setLastHurtByPlayer]
        methodNode.localVariables.clear()
        val instructions = VirtualClassPath[LivingEntity::setLastHurtByPlayer].instructions
        instructions.insert(buildInsnList {
            val continueLabel = instructions.first as LabelNode
            
            addLabel()
            aLoad(1)
            instanceOf(FakePlayer::class)
            ifne(continueLabel)
            addLabel()
            _return()
        })
    }
    
    private fun transformHurt() {
        val methodNode = VirtualClassPath[LivingEntity::hurt]
        methodNode.localVariables.clear()
        methodNode.replaceEvery(0, 0, {
            val popLabel = LabelNode()
            val continueLabel = LabelNode()
            
            // on stack: this, damageSource
            
            dup()
            instanceOf(FakePlayer::class)
            ifne(popLabel)
            addLabel()
            putField(LivingEntity::lastHurtByPlayer)
            goto(continueLabel)
            add(popLabel)
            pop2() // pops: this, damageSource
            add(continueLabel)
        }) { it is FieldInsnNode && it.puts(LivingEntity::lastHurtByPlayer) }
    }
    
}