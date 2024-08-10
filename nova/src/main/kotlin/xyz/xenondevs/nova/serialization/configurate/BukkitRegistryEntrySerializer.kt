package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.bukkit.Registry
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

internal inline fun <reified T : Keyed> Registry<T>.byNameTypeSerializer(): ScalarSerializer<T> {
    return BukkitRegistryEntrySerializer(this, geantyrefTypeTokenOf<T>())
}

internal class BukkitRegistryEntrySerializer<T : Keyed>(private val registry: Registry<T>, type: TypeToken<T>) : ScalarSerializer<T>(type) {
    
    override fun deserialize(type: Type?, obj: Any?): T {
        return registry.getOrThrow(Key.key(obj.toString()))
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>?): Any {
        return item.key().toString()
    }
    
}