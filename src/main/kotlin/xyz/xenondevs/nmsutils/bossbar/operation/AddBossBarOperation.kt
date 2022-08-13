package xyz.xenondevs.nmsutils.bossbar.operation

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.BossEvent
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

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
        
        return ReflectionRegistry.BOSS_BAR_ADD_OPERATION_CONSTRUCTOR.newInstance(buf)
    }
    
    companion object : Type<AddBossBarOperation> {
        
        override fun fromNMS(operation: Any): AddBossBarOperation {
            return AddBossBarOperation(
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_NAME_FIELD.get(operation) as Component,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_PROGRESS_FIELD.get(operation) as Float,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_COLOR_FIELD.get(operation) as BossEvent.BossBarColor,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_OVERLAY_FIELD.get(operation) as BossEvent.BossBarOverlay,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_DARKEN_SCREEN_FIELD.get(operation) as Boolean,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_PLAY_MUSIC_FIELD.get(operation) as Boolean,
                ReflectionRegistry.BOSS_BAR_ADD_OPERATION_CREATE_WORLD_FOG_FIELD.get(operation) as Boolean
            )
        }
        
    }
    
}