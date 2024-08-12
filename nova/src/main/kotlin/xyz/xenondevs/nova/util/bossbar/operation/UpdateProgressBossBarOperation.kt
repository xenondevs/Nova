package xyz.xenondevs.nova.util.bossbar.operation

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class UpdateProgressBossBarOperation(
    val progress: Float
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return UPDATE_PROGRESS_OPERATION_CONSTRUCTOR.invoke(progress)
    }
    
    companion object : Type<UpdateProgressBossBarOperation> {
        
        val UPDATE_PROGRESS_OPERATION: Class<*> = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateProgressOperation")
        private val UPDATE_PROGRESS_LOOKUP = MethodHandles
            .privateLookupIn(UPDATE_PROGRESS_OPERATION, MethodHandles.lookup())
        private val UPDATE_PROGRESS_OPERATION_CONSTRUCTOR = UPDATE_PROGRESS_LOOKUP
            .findConstructor(UPDATE_PROGRESS_OPERATION, MethodType.methodType(Void.TYPE, Float::class.java))
        private val UPDATE_PROGRESS_OPERATION_PROGRESS_GETTER = UPDATE_PROGRESS_LOOKUP
            .findGetter(UPDATE_PROGRESS_OPERATION, "progress", Float::class.java)
        
        override fun fromNMS(operation: Any): UpdateProgressBossBarOperation {
            return UpdateProgressBossBarOperation(UPDATE_PROGRESS_OPERATION_PROGRESS_GETTER.invoke(operation) as Float)
        }
        
    }
    
}