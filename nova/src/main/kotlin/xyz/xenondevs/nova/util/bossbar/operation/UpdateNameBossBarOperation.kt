package xyz.xenondevs.nova.util.bossbar.operation

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import net.minecraft.network.chat.Component as MojangComponent

class UpdateNameBossBarOperation(
    val name: Component
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        return UPDATE_NAME_OPERATION_CONSTRUCTOR.invoke(name.toNMSComponent())
    }
    
    companion object : Type<UpdateNameBossBarOperation> {
        
        val UPDATE_NAME_OPERATION: Class<*> = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateNameOperation")
        private val UPDATE_NAME_LOOKUP = MethodHandles
            .privateLookupIn(UPDATE_NAME_OPERATION, MethodHandles.lookup())
        private val UPDATE_NAME_OPERATION_CONSTRUCTOR = UPDATE_NAME_LOOKUP
            .findConstructor(UPDATE_NAME_OPERATION, MethodType.methodType(Void.TYPE, MojangComponent::class.java))
        private val UPDATE_NAME_OPERATION_NAME_GETTER = UPDATE_NAME_LOOKUP
            .findGetter(UPDATE_NAME_OPERATION, "name", MojangComponent::class.java)
        
        override fun fromNMS(operation: Any): UpdateNameBossBarOperation {
            val name = UPDATE_NAME_OPERATION_NAME_GETTER.invoke(operation) as MojangComponent
            return UpdateNameBossBarOperation(name.toAdventureComponent())
        }
        
    }
    
}