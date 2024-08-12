package xyz.xenondevs.nova.util.bossbar.operation

import net.minecraft.world.BossEvent
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class UpdateStyleBossBarOperation(
    val color: BossEvent.BossBarColor,
    val overlay: BossEvent.BossBarOverlay
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return UPDATE_STYLE_OPERATION_CONSTRUCTOR.invoke(color, overlay)
    }
    
    companion object : Type<UpdateStyleBossBarOperation> {
        
        val UPDATE_STYLE_OPERATION: Class<*> = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateStyleOperation")
        private val UPDATE_STYLE_LOOKUP = MethodHandles
            .privateLookupIn(UPDATE_STYLE_OPERATION, MethodHandles.lookup())
        private val UPDATE_STYLE_OPERATION_CONSTRUCTOR = UPDATE_STYLE_LOOKUP
            .findConstructor(
                UPDATE_STYLE_OPERATION,
                MethodType.methodType(
                    Void.TYPE,
                    BossEvent.BossBarColor::class.java, BossEvent.BossBarOverlay::class.java
                )
            )
        private val UPDATE_STYLE_OPERATION_COLOR_GETTER = UPDATE_STYLE_LOOKUP
            .findGetter(UPDATE_STYLE_OPERATION, "color", BossEvent.BossBarColor::class.java)
        private val UPDATE_STYLE_OPERATION_OVERLAY_GETTER = UPDATE_STYLE_LOOKUP
            .findGetter(UPDATE_STYLE_OPERATION, "overlay", BossEvent.BossBarOverlay::class.java)
        
        override fun fromNMS(operation: Any): UpdateStyleBossBarOperation {
            return UpdateStyleBossBarOperation(
                UPDATE_STYLE_OPERATION_COLOR_GETTER.invoke(operation) as BossEvent.BossBarColor,
                UPDATE_STYLE_OPERATION_OVERLAY_GETTER.invoke(operation) as BossEvent.BossBarOverlay
            )
        }
        
    }
    
}