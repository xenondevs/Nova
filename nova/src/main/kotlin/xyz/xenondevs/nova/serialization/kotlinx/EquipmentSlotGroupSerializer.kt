package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.EquipmentSlotGroup

internal object EquipmentSlotGroupSerializer : KSerializer<EquipmentSlotGroup> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.EquipmentSlotGroup", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: EquipmentSlotGroup) {
        encoder.encodeString(value.toString().lowercase())
    }
    
    override fun deserialize(decoder: Decoder): EquipmentSlotGroup {
        return EquipmentSlotGroup.getByName(decoder.decodeString())
            ?: throw SerializationException("Not an EquipmentSlotGroup: ${decoder.decodeString()}")
    }
    
}