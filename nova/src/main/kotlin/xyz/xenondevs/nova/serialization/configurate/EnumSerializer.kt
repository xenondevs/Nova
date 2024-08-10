package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.GenericTypeReflector
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.util.EnumLookup
import java.lang.reflect.Type
import java.util.function.Predicate
import kotlin.Enum

/**
 * An alternative enum serializer that lists available entries on failure.
 */
internal object EnumSerializer : ScalarSerializer<Enum<*>>(geantyrefTypeTokenOf<Enum<*>>()) {
    
    override fun deserialize(type: Type, obj: Any): Enum<*> {
        val entryName = obj.toString()
        val enumClass = GenericTypeReflector.erase(type).asSubclass(Enum::class.java)
        
        return EnumLookup.lookupEnum(enumClass, entryName)
            ?: throw SerializationException("Invalid enum constant: $entryName. Possible values: [${enumClass.enumConstants.joinToString { it.name }}]")
    }
    
    override fun serialize(item: Enum<*>, typeSupported: Predicate<Class<*>>): Any {
        return item.name
    }
    
}