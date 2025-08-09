package xyz.xenondevs.nova.network

import com.mojang.datafixers.util.Pair
import io.netty.buffer.Unpooled
import net.minecraft.core.Holder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nova.util.RegistryFriendlyByteBuf

fun ClientboundSetPassengersPacket(vehicle: Int, passengers: IntArray): ClientboundSetPassengersPacket {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    buffer.writeVarInt(vehicle)
    buffer.writeVarIntArray(passengers)
    return ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer)
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

fun ServerboundPlaceRecipePacket(containerId: Int, recipe: ResourceLocation, shiftDown: Boolean): ServerboundPlaceRecipePacket {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    buf.writeByte(containerId)
    buf.writeResourceLocation(recipe)
    buf.writeBoolean(shiftDown)
    return ServerboundPlaceRecipePacket.STREAM_CODEC.decode(buf)
}

fun ClientboundSetEquipmentPacket(entityId: Int, equipment: Map<EquipmentSlot, ItemStack>): ClientboundSetEquipmentPacket {
    val equipmentList = equipment.map { (slot, itemStack) -> Pair(slot, itemStack) }
    return ClientboundSetEquipmentPacket(entityId, equipmentList)
}