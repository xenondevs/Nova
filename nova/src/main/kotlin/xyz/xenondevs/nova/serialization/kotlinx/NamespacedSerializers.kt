package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

internal object KeySerializer : KSerializer<Key> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.KeySerializer", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Key {
        return Key.key(decoder.decodeString())
    }
    
}

internal object ResourceLocationSerializer : KSerializer<ResourceLocation> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.ResourceLocation", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): ResourceLocation {
        return ResourceLocation.parse(decoder.decodeString())
    }
    
}

internal object ResourceKeySerializer : KSerializer<ResourceKey<*>> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.ResourceKey", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: ResourceKey<*>) {
        encoder.encodeString(value.registry().toString() + ":" + value.location().toString())
    }
    
    override fun deserialize(decoder: Decoder): ResourceKey<*> {
        val (namespace1, name1, namespace2, name2) = decoder.decodeString().split(":")
        return ResourceKey.create(
            ResourceKey.createRegistryKey<Any>(ResourceLocation.fromNamespaceAndPath(namespace1, name1)),
            ResourceLocation.fromNamespaceAndPath(namespace2, name2)
        )
    }
    
}