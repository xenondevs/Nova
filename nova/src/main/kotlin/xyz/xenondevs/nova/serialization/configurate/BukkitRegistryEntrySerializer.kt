package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

internal inline fun <reified T : Keyed> RegistryKey<T>.byNameTypeSerializer(): ScalarSerializer<T> {
    return BukkitRegistryEntrySerializer(this, geantyrefTypeTokenOf<T>())
}

internal class BukkitRegistryEntrySerializer<T : Keyed>(private val registryKey: RegistryKey<T>, type: TypeToken<T>) : ScalarSerializer<T>(type) {
    
    private val registry by lazy { RegistryAccess.registryAccess().getRegistry(registryKey) }
    
    override fun deserialize(type: Type?, obj: Any?): T {
        return registry.getOrThrow(Key.key(obj.toString()))
    }
    
    override fun serialize(item: T, typeSupported: Predicate<Class<*>>?): Any {
        return item.key().toString()
    }
    
}