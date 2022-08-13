package xyz.xenondevs.nmsutils.bossbar.operation

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

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
            val operation = ReflectionRegistry.CLIENTBOUND_BOSS_EVENT_PACKET_OPERATION_FIELD.get(packet)
            
            val type = when (operation.javaClass) {
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_CLASS -> Type.ADD
                ReflectionRegistry.BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS -> Type.UPDATE_PROGRESS
                ReflectionRegistry.BOSS_BAR_UPDATE_NAME_OPERATION_CLASS -> Type.UPDATE_NAME
                ReflectionRegistry.BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS -> Type.UPDATE_STYLE
                ReflectionRegistry.BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS -> Type.UPDATE_PROPERTIES
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