package xyz.xenondevs.nova.util.bossbar.operation

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class UpdatePropertiesBossBarOperation(
    val darkenScreen: Boolean,
    val playMusic: Boolean,
    val createWorldFog: Boolean
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return UPDATE_PROPERTIES_OPERATION_CONSTRUCTOR.invoke(darkenScreen, playMusic, createWorldFog)
    }
    
    companion object : Type<UpdatePropertiesBossBarOperation> {
        
        val UPDATE_PROPERTIES_OPERATION: Class<*> = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation")
        private val UPDATE_PROPERTIES_LOOKUP = MethodHandles
            .privateLookupIn(UPDATE_PROPERTIES_OPERATION, MethodHandles.lookup())
        private val UPDATE_PROPERTIES_OPERATION_CONSTRUCTOR = UPDATE_PROPERTIES_LOOKUP
            .findConstructor(
                UPDATE_PROPERTIES_OPERATION,
                MethodType.methodType(
                    Void.TYPE,
                    Boolean::class.java, Boolean::class.java, Boolean::class.java
                )
            )
        private val UPDATE_PROPERTIES_OPERATION_DARKEN_SCREEN_GETTER = UPDATE_PROPERTIES_LOOKUP
            .findGetter(UPDATE_PROPERTIES_OPERATION, "darkenScreen", Boolean::class.java)
        private val UPDATE_PROPERTIES_OPERATION_PLAY_MUSIC_GETTER = UPDATE_PROPERTIES_LOOKUP
            .findGetter(UPDATE_PROPERTIES_OPERATION, "playMusic", Boolean::class.java)
        private val UPDATE_PROPERTIES_OPERATION_CREATE_WORLD_FOG_GETTER = UPDATE_PROPERTIES_LOOKUP
            .findGetter(UPDATE_PROPERTIES_OPERATION, "createWorldFog", Boolean::class.java)
        
        override fun fromNMS(operation: Any): UpdatePropertiesBossBarOperation {
            return UpdatePropertiesBossBarOperation(
                UPDATE_PROPERTIES_OPERATION_DARKEN_SCREEN_GETTER.invoke(operation) as Boolean,
                UPDATE_PROPERTIES_OPERATION_PLAY_MUSIC_GETTER.invoke(operation) as Boolean,
                UPDATE_PROPERTIES_OPERATION_CREATE_WORLD_FOG_GETTER.invoke(operation) as Boolean
            )
        }
        
    }
    
}