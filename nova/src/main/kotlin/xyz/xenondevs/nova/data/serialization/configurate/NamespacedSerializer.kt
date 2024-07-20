package xyz.xenondevs.nova.data.serialization.configurate

import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.data.resources.ResourcePath
import java.lang.reflect.Type
import java.util.function.Predicate

internal object NamespacedKeySerializer : ScalarSerializer<NamespacedKey>(NamespacedKey::class.java) {
    
    override fun deserialize(type: Type, obj: Any): NamespacedKey {
        return NamespacedKey.fromString(obj.toString()) ?: throw IllegalArgumentException("Invalid key: $obj")
    }
    
    override fun serialize(item: NamespacedKey, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}

internal object ResourceLocationSerializer : ScalarSerializer<ResourceLocation>(ResourceLocation::class.java) {
    
    override fun deserialize(type: Type, obj: Any): ResourceLocation {
        return ResourceLocation.parse(obj.toString())
    }
    
    override fun serialize(item: ResourceLocation, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}

internal object ResourcePathSerializer : ScalarSerializer<ResourcePath>(ResourcePath::class.java) {
    
    override fun deserialize(type: Type, obj: Any): ResourcePath {
        return ResourcePath.of(obj.toString())
    }
    
    override fun serialize(item: ResourcePath, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}