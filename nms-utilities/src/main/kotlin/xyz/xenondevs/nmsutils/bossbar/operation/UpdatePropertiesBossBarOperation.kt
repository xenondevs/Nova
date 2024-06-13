package xyz.xenondevs.nmsutils.bossbar.operation

import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

class UpdatePropertiesBossBarOperation(
    val darkenScreen: Boolean,
    val playMusic: Boolean,
    val createWorldFog: Boolean
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return ReflectionRegistry.BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CONSTRUCTOR.newInstance(
            darkenScreen, playMusic, createWorldFog
        )
    }
    
    companion object : Type<UpdatePropertiesBossBarOperation> {
        
        override fun fromNMS(operation: Any): UpdatePropertiesBossBarOperation {
            return UpdatePropertiesBossBarOperation(
                ReflectionRegistry.BOSS_BAR_UPDATE_PROPERTIES_OPERATION_DARKEN_SCREEN_FIELD.get(operation) as Boolean,
                ReflectionRegistry.BOSS_BAR_UPDATE_PROPERTIES_OPERATION_PLAY_MUSIC_FIELD.get(operation) as Boolean,
                ReflectionRegistry.BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CREATE_WORLD_FOG_FIELD.get(operation) as Boolean
            )
        }
        
    }
    
}