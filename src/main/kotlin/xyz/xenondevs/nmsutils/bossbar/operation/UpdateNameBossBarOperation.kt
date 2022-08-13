package xyz.xenondevs.nmsutils.bossbar.operation

import net.minecraft.network.chat.Component
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

class UpdateNameBossBarOperation(
    val name: Component
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return ReflectionRegistry.BOSS_BAR_UPDATE_NAME_OPERATION_CONSTRUCTOR.newInstance(name)
    }
    
    companion object : Type<UpdateNameBossBarOperation> {
        
        override fun fromNMS(operation: Any): UpdateNameBossBarOperation {
            return UpdateNameBossBarOperation(
                ReflectionRegistry.BOSS_BAR_UPDATE_NAME_OPERATION_NAME_FIELD.get(operation) as Component
            )
        }
        
    }
    
}