package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.Equipment

internal object NovaBlockSerializer : NmsRegistryEntrySerializer<NovaBlock>(NovaRegistries.BLOCK)
internal object EquipmentSerializer : NmsRegistryEntrySerializer<Equipment>(NovaRegistries.EQUIPMENT)
internal object GuiTextureSerializer : NmsRegistryEntrySerializer<GuiTexture>(NovaRegistries.GUI_TEXTURE)

internal object BlockSerializer : NmsRegistryEntrySerializer<Block>(BuiltInRegistries.BLOCK)

internal abstract class NmsRegistryEntrySerializer<T : Any>(val registry: Registry<T>) : KSerializer<T> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.RegistryEntrySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: T) {
        val id = registry.getKey(value) 
            ?: throw SerializationException("Value $value is not registered in $registry")
        encoder.encodeString(id.toString())
    }
    
    override fun deserialize(decoder: Decoder): T {
        val id = decoder.decodeString()
        return registry.getValue(id)
            ?: throw SerializationException("No entry under $id in $registry")
    }
    
}