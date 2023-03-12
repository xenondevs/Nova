@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.bossbar

import net.minecraft.world.BossEvent
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object BossBarOriginPatch : MethodTransformer(ReflectionRegistry.CLIENTBOUND_BOSS_EVENT_PACKET_CREATE_ADD_PACKET_METHOD) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeStatic(ReflectionUtils.getMethodByName(BossBarOriginPatch::class.java, false, "handleBossBarAddPacket"))
        })
    }
    
    @JvmStatic
    fun handleBossBarAddPacket(event: BossEvent) {
        BossBarOverlayManager.handleBossBarAddPacketCreation(event)
    }
    
}