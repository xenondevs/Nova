package xyz.xenondevs.nmsutils.bossbar.operation

import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

class UpdateProgressBossBarOperation(
    val progress: Float
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return ReflectionRegistry.BOSS_BAR_UPDATE_PROGRESS_OPERATION_CONSTRUCTOR.newInstance(progress)
    }
    
    companion object : Type<UpdateProgressBossBarOperation> {
        
        override fun fromNMS(operation: Any): UpdateProgressBossBarOperation {
            return UpdateProgressBossBarOperation(
                ReflectionRegistry.BOSS_BAR_UPDATE_PROGRESS_OPERATION_PROGRESS_FIELD.get(operation) as Float
            )
        }
        
    }
    
}