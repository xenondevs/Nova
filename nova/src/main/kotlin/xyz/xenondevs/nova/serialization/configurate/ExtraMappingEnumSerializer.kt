package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.util.EnumLookup
import xyz.xenondevs.nova.util.data.geantyrefTypeTokenOf
import java.lang.reflect.Type
import java.util.function.Predicate

internal inline fun <reified E : Enum<E>> ExtraMappingEnumSerializer(vararg extraMappings: Pair<String, E>) =
    ExtraMappingEnumSerializer(geantyrefTypeTokenOf<E>(), extraMappings.toMap())

internal class ExtraMappingEnumSerializer<E : Enum<E>>(
    token: TypeToken<E>,
    extraMappings: Map<String, E>
) : ScalarSerializer<E>(token) {
    
    private val extraMappings: Map<String, E> = extraMappings.mapKeys { it.key.lowercase() }
    
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(type: Type, obj: Any): E {
        val entryName = obj.toString()
        val enumClass = GenericTypeReflector.erase(type).asSubclass(Enum::class.java)
        
        val result = extraMappings[entryName.lowercase()]
            ?: EnumLookup.lookupEnum(enumClass, entryName) as? E
        
        if (result == null) {
            val values = enumClass.enumConstants.joinToString(", ") { constant ->
                val extraMappings = extraMappings.entries
                    .filter { it.value == constant }
                    .map { it.key }
                
                constant.name + extraMappings.joinToString(", ", " (", ")")
            }
            
            throw SerializationException("Invalid enum constant: $entryName. Possible values: [$values]")
        }
        
        return result
    }
    
    override fun serialize(item: E, typeSupported: Predicate<Class<*>>): Any {
        return item.name
    }
    
}