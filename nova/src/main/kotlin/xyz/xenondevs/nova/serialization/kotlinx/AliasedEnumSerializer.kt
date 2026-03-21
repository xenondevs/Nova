package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base class for enum serializers that performs case-insensitive name lookup
 * and supports extra name mappings. Throws [SerializationException] on invalid input
 * with a message listing all possible values.
 */
internal open class AliasedEnumSerializer<E : Enum<E>>(
    serialName: String,
    entries: Array<E>,
    extraMappings: Map<String, E> = emptyMap()
) : KSerializer<E> {
    
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
    
    private val byName = HashMap<String, E>().also { map ->
        entries.associateByTo(map) { it.name.lowercase() }
        extraMappings.forEach { (k, v) -> map[k.lowercase()] = v }
    }
    
    private val possibleValues = entries.joinToString { entry ->
        val aliases = extraMappings.entries.filter { (_, v) -> v == entry }.map { (k, _) -> k }
        if (aliases.isEmpty()) entry.name else "${entry.name} (${aliases.joinToString()})"
    }
    
    override fun deserialize(decoder: Decoder): E {
        val str = decoder.decodeString()
        return byName[str.lowercase()]
            ?: throw SerializationException("Unknown value: $str. Possible values: [$possibleValues]")
    }
    
    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(value.name)
    }
    
}
