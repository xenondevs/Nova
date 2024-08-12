package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import net.minecraft.core.Registry
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.util.getOrThrow
import java.lang.reflect.Type
import java.util.function.Predicate

@PublishedApi
internal class RegistryEntrySerializer<T : Any>(private val registry: Registry<T>, type: TypeToken<T>) : ScalarSerializer<T>(type) {
    
    override fun deserialize(type: Type, obj: Any): T {
        return registry.getOrThrow(obj.toString())
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>): Any {
        val id = registry.getKey(item) ?: throw IllegalArgumentException("Item $item is not registered in $registry")
        return id.toString()
    }
    
}