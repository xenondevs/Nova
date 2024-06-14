package xyz.xenondevs.nova.util.bossbar.operation

import io.netty.buffer.Unpooled
import net.kyori.adventure.text.Component
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.BossEvent
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import net.minecraft.network.chat.Component as MojangComponent

class AddBossBarOperation(
    val name: Component,
    val progress: Float,
    val color: BossEvent.BossBarColor,
    val overlay: BossEvent.BossBarOverlay,
    val darkenScreen: Boolean,
    val playMusic: Boolean,
    val createWorldFog: Boolean
) : BossBarOperation() {
    
    override fun toNMS(): Any {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeComponent(name)
        buf.writeFloat(progress)
        buf.writeEnum(color)
        buf.writeEnum(overlay)
        buf.writeByte(encodeProperties(darkenScreen, playMusic, createWorldFog))
        
        return ADD_OPERATION_CONSTRUCTOR.invoke(buf)
    }
    
    companion object : Type<AddBossBarOperation> {
        
        val ADD_OPERATION: Class<*> = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation")
        private val ADD_OPERATION_LOOKUP = MethodHandles
            .privateLookupIn(ADD_OPERATION, MethodHandles.lookup())
        private val ADD_OPERATION_CONSTRUCTOR = ADD_OPERATION_LOOKUP
            .findConstructor(ADD_OPERATION, MethodType.methodType(Void.TYPE, FriendlyByteBuf::class.java))
        private val ADD_OPERATION_NAME_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "name", MojangComponent::class.java)
        private val ADD_OPERATION_PROGRESS_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "progress", Float::class.java)
        private val ADD_OPERATION_COLOR_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "color", BossEvent.BossBarColor::class.java)
        private val ADD_OPERATION_OVERLAY_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "overlay", BossEvent.BossBarOverlay::class.java)
        private val ADD_OPERATION_DARKEN_SCREEN_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "darkenScreen", Boolean::class.java)
        private val ADD_OPERATION_PLAY_MUSIC_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "playMusic", Boolean::class.java)
        private val ADD_OPERATION_CREATE_WORLD_FOG_GETTER = ADD_OPERATION_LOOKUP
            .findGetter(ADD_OPERATION, "createWorldFog", Boolean::class.java)
        
        override fun fromNMS(operation: Any): AddBossBarOperation {
            return AddBossBarOperation(
                (ADD_OPERATION_NAME_GETTER.invoke(operation) as MojangComponent).toAdventureComponent(),
                ADD_OPERATION_PROGRESS_GETTER.invoke(operation) as Float,
                ADD_OPERATION_COLOR_GETTER.invoke(operation) as BossEvent.BossBarColor,
                ADD_OPERATION_OVERLAY_GETTER.invoke(operation) as BossEvent.BossBarOverlay,
                ADD_OPERATION_DARKEN_SCREEN_GETTER.invoke(operation) as Boolean,
                ADD_OPERATION_PLAY_MUSIC_GETTER.invoke(operation) as Boolean,
                ADD_OPERATION_CREATE_WORLD_FOG_GETTER.invoke(operation) as Boolean
            )
        }
        
    }
    
}