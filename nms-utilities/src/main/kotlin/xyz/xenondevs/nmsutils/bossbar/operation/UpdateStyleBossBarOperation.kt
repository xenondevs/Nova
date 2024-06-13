package xyz.xenondevs.nmsutils.bossbar.operation

import net.minecraft.world.BossEvent
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

class UpdateStyleBossBarOperation(
    val color: BossEvent.BossBarColor,
    val overlay: BossEvent.BossBarOverlay
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return ReflectionRegistry.BOSS_BAR_UPDATE_STYLE_OPERATION_CONSTRUCTOR.newInstance(color, overlay)
    }
    
    companion object : Type<UpdateStyleBossBarOperation> {
        
        override fun fromNMS(operation: Any): UpdateStyleBossBarOperation {
            return UpdateStyleBossBarOperation(
                ReflectionRegistry.BOSS_BAR_UPDATE_STYLE_OPERATION_COLOR_FIELD.get(operation) as BossEvent.BossBarColor,
                ReflectionRegistry.BOSS_BAR_UPDATE_STYLE_OPERATION_OVERLAY_FIELD.get(operation) as BossEvent.BossBarOverlay
            )
        }
        
    }
    
}