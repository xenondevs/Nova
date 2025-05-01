package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.util.data.geantyrefTypeTokenOf
import xyz.xenondevs.nova.util.getValueOrThrow
import java.lang.reflect.Type
import java.util.function.Predicate

internal inline fun <reified T : Any> RegistryEntrySerializer(registry: Registry<T>) =
    RegistryEntrySerializer(registry, geantyrefTypeTokenOf<T>())

internal class RegistryEntrySerializer<T : Any>(private val registry: Registry<T>, type: TypeToken<T>) : ScalarSerializer<T>(type) {
    
    override fun deserialize(type: Type, obj: Any): T {
        return registry.getValueOrThrow(obj.toString().lowercase())
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>): Any {
        val id = registry.getKey(item) ?: throw IllegalArgumentException("Item $item is not registered in $registry")
        return id.toString()
    }
    
}