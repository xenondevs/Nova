package xyz.xenondevs.nova.network.packet

import com.mojang.datafixers.util.Pair
import io.netty.buffer.Unpooled
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.Holder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf as MojangRegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.network.chat.numbers.NumberFormatTypes
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.login.ServerboundKeyPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import xyz.xenondevs.nova.network.RegistryFriendlyByteBuf
import java.util.Optional

fun ClientboundSetPassengersPacket(
    vehicle: Int,
    passengers: IntArray
): ClientboundSetPassengersPacket = decodePacket(ClientboundSetPassengersPacket.STREAM_CODEC) {
    writeVarInt(vehicle)
    writeVarIntArray(passengers)
}

fun ClientboundSoundEntityPacket(
    sound: Holder<SoundEvent>,
    source: SoundSource,
    entityId: Int,
    volume: Float,
    pitch: Float,
    seed: Long
): ClientboundSoundEntityPacket = decodeRegistryPacket(ClientboundSoundEntityPacket.STREAM_CODEC) {
    SoundEvent.STREAM_CODEC.encode(this, sound)
    writeEnum(source)
    writeVarInt(entityId)
    writeFloat(volume)
    writeFloat(pitch)
    writeLong(seed)
}

fun ServerboundPlaceRecipePacket(
    containerId: Int,
    recipe: Identifier,
    shiftDown: Boolean
): ServerboundPlaceRecipePacket = decodePacket(ServerboundPlaceRecipePacket.STREAM_CODEC) {
    writeByte(containerId)
    writeIdentifier(recipe)
    writeBoolean(shiftDown)
}

fun ClientboundSetEquipmentPacket(entityId: Int, equipment: Map<EquipmentSlot, ItemStack>): ClientboundSetEquipmentPacket {
    val equipmentList = equipment.map { [slot, itemStack] -> Pair(slot, itemStack) }
    return ClientboundSetEquipmentPacket(entityId, equipmentList)
}

fun ServerboundKeyPacket(
    keyBytes: ByteArray,
    encryptedChallenge: ByteArray
): ServerboundKeyPacket = decodePacket(ServerboundKeyPacket.STREAM_CODEC) {
    writeByteArray(keyBytes)
    writeByteArray(encryptedChallenge)
}

fun ClientboundAnimatePacket(
    entityId: Int,
    action: Int
): ClientboundAnimatePacket = decodePacket(ClientboundAnimatePacket.STREAM_CODEC) {
    writeVarInt(entityId)
    writeByte(action)
}

fun ClientboundCommandsPacket(
    entries: List<ClientboundCommandsPacket.Entry>,
    rootIndex: Int
): ClientboundCommandsPacket = decodePacket(ClientboundCommandsPacket.STREAM_CODEC) {
    writeCollection(entries) { buffer, entry -> entry.write(buffer) }
    writeVarInt(rootIndex)
}

fun ClientboundEntityEventPacket(
    entityId: Int,
    eventId: Byte
): ClientboundEntityEventPacket = decodePacket(ClientboundEntityEventPacket.STREAM_CODEC) {
    writeInt(entityId)
    writeByte(eventId.toInt())
}

fun ClientboundInitializeBorderPacket(
    newCenterX: Double,
    newCenterZ: Double,
    oldSize: Double,
    newSize: Double,
    lerpTime: Long,
    newAbsoluteMaxSize: Int,
    warningBlocks: Int,
    warningTime: Int
): ClientboundInitializeBorderPacket = decodePacket(ClientboundInitializeBorderPacket.STREAM_CODEC) {
    writeDouble(newCenterX)
    writeDouble(newCenterZ)
    writeDouble(oldSize)
    writeDouble(newSize)
    writeVarLong(lerpTime)
    writeVarInt(newAbsoluteMaxSize)
    writeVarInt(warningBlocks)
    writeVarInt(warningTime)
}

fun ClientboundLevelChunkWithLightPacket(
    x: Int,
    z: Int,
    chunkData: ClientboundLevelChunkPacketData,
    lightData: ClientboundLightUpdatePacketData,
    ready: Boolean
): ClientboundLevelChunkWithLightPacket =
    decodeRegistryPacket(ClientboundLevelChunkWithLightPacket.STREAM_CODEC) {
        writeInt(x)
        writeInt(z)
        chunkData.write(this)
        lightData.write(this)
    }.also {
        it.isReady = ready
    }

fun ClientboundLightUpdatePacket(
    x: Int,
    z: Int,
    lightData: ClientboundLightUpdatePacketData
): ClientboundLightUpdatePacket = decodePacket(ClientboundLightUpdatePacket.STREAM_CODEC) {
    writeVarInt(x)
    writeVarInt(z)
    lightData.write(this)
}

fun ClientboundPlayerAbilitiesPacket(
    invulnerable: Boolean,
    flying: Boolean,
    canFly: Boolean,
    instabuild: Boolean,
    flyingSpeed: Float,
    walkingSpeed: Float
): ClientboundPlayerAbilitiesPacket = decodePacket(ClientboundPlayerAbilitiesPacket.STREAM_CODEC) {
    var flags = 0
    if (invulnerable) flags = flags or 1
    if (flying) flags = flags or 2
    if (canFly) flags = flags or 4
    if (instabuild) flags = flags or 8
    writeByte(flags)
    writeFloat(flyingSpeed)
    writeFloat(walkingSpeed)
}

fun ClientboundSetBorderCenterPacket(
    newCenterX: Double,
    newCenterZ: Double
): ClientboundSetBorderCenterPacket = decodePacket(ClientboundSetBorderCenterPacket.STREAM_CODEC) {
    writeDouble(newCenterX)
    writeDouble(newCenterZ)
}

fun ClientboundSetBorderLerpSizePacket(
    oldSize: Double,
    newSize: Double,
    lerpTime: Long
): ClientboundSetBorderLerpSizePacket = decodePacket(ClientboundSetBorderLerpSizePacket.STREAM_CODEC) {
    writeDouble(oldSize)
    writeDouble(newSize)
    writeVarLong(lerpTime)
}

fun ClientboundSetBorderSizePacket(size: Double): ClientboundSetBorderSizePacket =
    decodePacket(ClientboundSetBorderSizePacket.STREAM_CODEC) {
        writeDouble(size)
    }

fun ClientboundSetBorderWarningDelayPacket(warningDelay: Int): ClientboundSetBorderWarningDelayPacket =
    decodePacket(ClientboundSetBorderWarningDelayPacket.STREAM_CODEC) {
        writeVarInt(warningDelay)
    }

fun ClientboundSetBorderWarningDistancePacket(warningBlocks: Int): ClientboundSetBorderWarningDistancePacket =
    decodePacket(ClientboundSetBorderWarningDistancePacket.STREAM_CODEC) {
        writeVarInt(warningBlocks)
    }

fun ClientboundSetCameraPacket(cameraId: Int): ClientboundSetCameraPacket =
    decodePacket(ClientboundSetCameraPacket.STREAM_CODEC) {
        writeVarInt(cameraId)
    }

fun ClientboundSetDisplayObjectivePacket(
    slot: DisplaySlot,
    objectiveName: String?
): ClientboundSetDisplayObjectivePacket = decodePacket(ClientboundSetDisplayObjectivePacket.STREAM_CODEC) {
    writeById(DisplaySlot::id, slot)
    writeUtf(objectiveName ?: "")
}

fun ClientboundSetEntityLinkPacket(
    sourceId: Int,
    destinationId: Int
): ClientboundSetEntityLinkPacket = decodePacket(ClientboundSetEntityLinkPacket.STREAM_CODEC) {
    writeInt(sourceId)
    writeInt(destinationId)
}

fun ClientboundPlayerLookAtPacket(
    fromAnchor: EntityAnchorArgument.Anchor,
    x: Double,
    y: Double,
    z: Double,
    entityId: Int?,
    toAnchor: EntityAnchorArgument.Anchor?
): ClientboundPlayerLookAtPacket = decodePacket(ClientboundPlayerLookAtPacket.STREAM_CODEC) {
    writeEnum(fromAnchor)
    writeDouble(x)
    writeDouble(y)
    writeDouble(z)
    writeBoolean(entityId != null)
    if (entityId != null) {
        writeVarInt(entityId)
        writeEnum(requireNotNull(toAnchor))
    }
}

fun ClientboundRotateHeadPacket(
    entityId: Int,
    packedYHeadRot: Byte
): ClientboundRotateHeadPacket = decodePacket(ClientboundRotateHeadPacket.STREAM_CODEC) {
    writeVarInt(entityId)
    writeByte(packedYHeadRot.toInt())
}

fun ServerboundPlayerAbilitiesPacket(flying: Boolean): ServerboundPlayerAbilitiesPacket =
    decodePacket(ServerboundPlayerAbilitiesPacket.STREAM_CODEC) {
        writeByte(if (flying) 2 else 0)
    }

fun ServerboundPlayerCommandPacket(
    entityId: Int,
    action: ServerboundPlayerCommandPacket.Action,
    data: Int
): ServerboundPlayerCommandPacket = decodePacket(ServerboundPlayerCommandPacket.STREAM_CODEC) {
    writeVarInt(entityId)
    writeEnum(action)
    writeVarInt(data)
}

fun ClientboundSetObjectivePacket(
    objectiveName: String,
    displayName: Component,
    renderType: ObjectiveCriteria.RenderType,
    numberFormat: Optional<NumberFormat>,
    method: Int
): ClientboundSetObjectivePacket =
    decodeRegistryPacket(ClientboundSetObjectivePacket.STREAM_CODEC) {
        writeUtf(objectiveName)
        writeByte(method)
        if (method == 0 || method == 2) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(this, displayName)
            writeEnum(renderType)
            NumberFormatTypes.OPTIONAL_STREAM_CODEC.encode(this, numberFormat)
        }
    }

private inline fun <P : Any> decodePacket(
    codec: StreamCodec<FriendlyByteBuf, P>,
    write: FriendlyByteBuf.() -> Unit
): P {
    val buffer = FriendlyByteBuf(Unpooled.buffer())
    return try {
        buffer.write()
        codec.decode(buffer)
    } finally {
        buffer.release()
    }
}

private inline fun <P : Any> decodeRegistryPacket(
    codec: StreamCodec<MojangRegistryFriendlyByteBuf, P>,
    write: MojangRegistryFriendlyByteBuf.() -> Unit
): P {
    val buffer = RegistryFriendlyByteBuf()
    return try {
        buffer.write()
        codec.decode(buffer)
    } finally {
        buffer.release()
    }
}
