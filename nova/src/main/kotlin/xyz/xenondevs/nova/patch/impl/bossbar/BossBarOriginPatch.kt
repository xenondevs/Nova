@file:Suppress("unused")

package xyz.xenondevs.nova.patch.impl.bossbar

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.world.BossEvent
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.MethodTransformer
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager

internal object BossBarOriginPatch : MethodTransformer(ClientboundBossEventPacket::createAddPacket) {
    
    override fun transform() {
        methodNode.instructions.insert(buildInsnList {
            addLabel()
            aLoad(0)
            invokeStatic(::handleBossBarAddPacket)
        })
    }
    
    @JvmStatic
    fun handleBossBarAddPacket(event: BossEvent) {
        BossBarOverlayManager.handleBossBarAddPacketCreation(event)
    }
    
}