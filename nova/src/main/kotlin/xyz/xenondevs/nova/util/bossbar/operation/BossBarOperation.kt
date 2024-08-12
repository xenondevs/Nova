package xyz.xenondevs.nova.util.bossbar.operation

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import java.lang.invoke.MethodHandles

private val OPERATION = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation")
private val CLIENTBOUND_BOSS_EVENT_PACKET_OPERATION_GETTER = MethodHandles
    .privateLookupIn(ClientboundBossEventPacket::class.java, MethodHandles.lookup())
    .findGetter(ClientboundBossEventPacket::class.java, "operation", OPERATION)

abstract class BossBarOperation {
    
    abstract fun toNMS(): Any
    
    companion object {
        
        internal fun encodeProperties(darkenScreen: Boolean, playMusic: Boolean, createWorldFog: Boolean): Int {
            var result = 0
            if (darkenScreen)
                result = result or 1
            if (playMusic)
                result = result or 2
            if (createWorldFog)
                result = result or 4
            return result
        }
        
        fun fromPacket(packet: ClientboundBossEventPacket): BossBarOperation {
            val operation = CLIENTBOUND_BOSS_EVENT_PACKET_OPERATION_GETTER.invoke(packet)
            
            val type = when (operation.javaClass) {
                AddBossBarOperation.ADD_OPERATION -> Type.ADD
                UpdateProgressBossBarOperation.UPDATE_PROGRESS_OPERATION -> Type.UPDATE_PROGRESS
                UpdateNameBossBarOperation.UPDATE_NAME_OPERATION -> Type.UPDATE_NAME
                UpdateStyleBossBarOperation.UPDATE_STYLE_OPERATION -> Type.UPDATE_STYLE
                UpdatePropertiesBossBarOperation.UPDATE_PROPERTIES_OPERATION -> Type.UPDATE_PROPERTIES
                else -> Type.REMOVE // anonymous class
            }
            
            return type.fromNMS(operation)
        }
        
    }
    
    sealed interface Type<O : BossBarOperation> {
        
        fun fromNMS(operation: Any): O
        
        companion object {
            val ADD = AddBossBarOperation
            val REMOVE = RemoveBossBarOperation
            val UPDATE_PROGRESS = UpdateProgressBossBarOperation
            val UPDATE_NAME = UpdateNameBossBarOperation
            val UPDATE_STYLE = UpdateStyleBossBarOperation
            val UPDATE_PROPERTIES = UpdatePropertiesBossBarOperation
        }
        
    }
    
}