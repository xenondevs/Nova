package xyz.xenondevs.nova.network

import io.netty.buffer.Unpooled
import net.minecraft.core.Holder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import xyz.xenondevs.nova.network.event.clientbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.util.RegistryFriendlyByteBuf
import xyz.xenondevs.nova.util.bossbar.operation.BossBarOperation
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.*

fun ClientboundSetPassengersPacket(vehicle: Int, passengers: IntArray): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(vehicle)
    buffer.writeVarIntArray(passengers)
    return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
}

private val BOSS_BAR_OPERATION_CLASS = Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation")
private val CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR = MethodHandles
    .privateLookupIn(ClientboundBossEventPacket::class.java, MethodHandles.lookup())
    .findConstructor(ClientboundBossEventPacket::class.java, MethodType.methodType(Void.TYPE, UUID::class.java, BOSS_BAR_OPERATION_CLASS))

fun ClientboundBossEventPacket(id: UUID, operation: BossBarOperation): ClientboundBossEventPacket {
    return CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR.invoke(id, operation.toNMS()) as ClientboundBossEventPacket
}

fun ClientboundSoundEntityPacket(sound: Holder<SoundEvent>, source: SoundSource, entityId: Int, volume: Float, pitch: Float, seed: Long): ClientboundSoundEntityPacket {
    val buf = RegistryFriendlyByteBuf()
    SoundEvent.STREAM_CODEC.encode(buf, sound)
    buf.writeEnum(source)
    buf.writeVarInt(entityId)
    buf.writeFloat(volume)
    buf.writeFloat(pitch)
    buf.writeLong(seed)
    
    return ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf)
}

private val CLIENTBOUND_UPDATE_ATTRIBUTES_PACKET_CONSTRUCTOR = MethodHandles
    .privateLookupIn(ClientboundUpdateAttributesPacket::class.java, MethodHandles.lookup())
    .findConstructor(ClientboundUpdateAttributesPacket::class.java, MethodType.methodType(Void.TYPE, Int::class.java, List::class.java))

fun ClientboundUpdateAttributesPacket(entityId: Int, values: List<AttributeSnapshot>): ClientboundUpdateAttributesPacket {
    return CLIENTBOUND_UPDATE_ATTRIBUTES_PACKET_CONSTRUCTOR.invoke(entityId, values) as ClientboundUpdateAttributesPacket
}

fun ServerboundPlaceRecipePacket(containerId: Int, recipe: ResourceLocation, shiftDown: Boolean): ServerboundPlaceRecipePacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeByte(containerId)
    buf.writeResourceLocation(recipe)
    buf.writeBoolean(shiftDown)
    return ServerboundPlaceRecipePacket.STREAM_CODEC.decode(buf)
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
    
    return ServerboundInteractPacket.STREAM_CODEC.decode(buf)
}