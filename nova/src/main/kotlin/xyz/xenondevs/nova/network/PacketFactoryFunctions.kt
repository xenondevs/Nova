package xyz.xenondevs.nova.network

import io.netty.buffer.Unpooled
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import xyz.xenondevs.nova.network.event.clientbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.util.bossbar.operation.BossBarOperation
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.util.toPackedByte
import java.util.*

private val BOSS_BAR_OPERATION_CLASS = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation").kotlin
private val CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR = ReflectionUtils.getConstructor(ClientboundBossEventPacket::class, UUID::class, BOSS_BAR_OPERATION_CLASS)

fun ClientboundPlaceGhostRecipePacket(containerId: Int, recipe: ResourceLocation): ClientboundPlaceGhostRecipePacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeByte(containerId)
    buffer.writeResourceLocation(recipe)
    return ClientboundPlaceGhostRecipePacket(buffer)
}

fun ClientboundSetPassengersPacket(vehicle: Int, passengers: IntArray): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(vehicle)
    buffer.writeVarIntArray(passengers)
    return ClientboundSetPassengersPacket(buffer)
}

fun ClientboundRotateHeadPacket(entity: Int, yaw: Float): ClientboundRotateHeadPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(entity)
    buffer.writeByte(yaw.toPackedByte().toInt())
    return ClientboundRotateHeadPacket(buffer)
}

fun ClientboundBossEventPacket(id: UUID, operation: BossBarOperation): ClientboundBossEventPacket {
    return CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR.newInstance(id, operation.toNMS())
}

fun ClientboundTeleportEntityPacket(entityId: Int, x: Double, y: Double, z: Double, yaw: Float, pitch: Float, isOnGround: Boolean): ClientboundTeleportEntityPacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeVarInt(entityId)
    buf.writeDouble(x)
    buf.writeDouble(y)
    buf.writeDouble(z)
    buf.writeByte(yaw.toPackedByte().toInt())
    buf.writeByte(pitch.toPackedByte().toInt())
    buf.writeBoolean(isOnGround)
    
    return ClientboundTeleportEntityPacket(buf)
}

fun ClientboundSoundEntityPacket(sound: Holder<SoundEvent>, source: SoundSource, entityId: Int, volume: Float, pitch: Float, seed: Long): ClientboundSoundEntityPacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeId(BuiltInRegistries.SOUND_EVENT.asHolderIdMap(), sound) { soundEvent, buffer -> buffer.writeToNetwork(soundEvent) }
    buf.writeEnum(source)
    buf.writeVarInt(entityId)
    buf.writeFloat(volume)
    buf.writeFloat(pitch)
    buf.writeLong(seed)
    
    return ClientboundSoundEntityPacket(buf)
}

fun ServerboundPlaceRecipePacket(containerId: Int, recipe: ResourceLocation, shiftDown: Boolean): ServerboundPlaceRecipePacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeByte(containerId)
    buf.writeResourceLocation(recipe)
    buf.writeBoolean(shiftDown)
    return ServerboundPlaceRecipePacket(buf)
}

fun ServerboundInteractPacket(entityId: Int, action: ServerboundInteractPacketEvent.Action, isUsingSecondaryAction: Boolean): ServerboundInteractPacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    
    buf.writeVarInt(entityId)
    when (action) {
        is ServerboundInteractPacketEvent.Action.Interact -> {
            buf.writeVarInt(0)
            buf.writeVarInt(action.hand.ordinal)
        }
        
        is ServerboundInteractPacketEvent.Action.InteractAtLocation -> {
            buf.writeVarInt(2)
            buf.writeFloat(action.location.x.toFloat())
            buf.writeFloat(action.location.y.toFloat())
            buf.writeFloat(action.location.z.toFloat())
            buf.writeVarInt(action.hand.ordinal)
        }
        
        is ServerboundInteractPacketEvent.Action.Attack -> {
            buf.writeVarInt(1)
        }
    }
    buf.writeBoolean(isUsingSecondaryAction)
    
    return ServerboundInteractPacket(buf)
}