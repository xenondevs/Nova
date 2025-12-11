package xyz.xenondevs.nova.serialization.configurate

import net.kyori.adventure.key.Key
import net.minecraft.resources.Identifier
import org.bukkit.NamespacedKey
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.util.data.geantyrefTypeTokenOf
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Predicate

internal object KeySerializer : ScalarSerializer<Key>(Key::class.java) {
    
    override fun deserialize(type: Type, obj: Any): Key {
        return Key.key(obj.toString())
    }
    
    override fun serialize(item: Key, typeSupported: Predicate<Class<*>>): Any? {
        return item.asString()
    }
    
}

internal object NamespacedKeySerializer : ScalarSerializer<NamespacedKey>(NamespacedKey::class.java) {
    
    override fun deserialize(type: Type, obj: Any): NamespacedKey {
        return NamespacedKey.fromString(obj.toString()) ?: throw IllegalArgumentException("Invalid key: $obj")
    }
    
    override fun serialize(item: NamespacedKey, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}

internal object IdentifierSerializer : ScalarSerializer<Identifier>(Identifier::class.java) {
    
    override fun deserialize(type: Type, obj: Any): Identifier {
        return Identifier.parse(obj.toString())
    }
    
    override fun serialize(item: Identifier, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}

internal object ResourcePathSerializer : ScalarSerializer<ResourcePath<*>>(geantyrefTypeTokenOf<ResourcePath<*>>()) {
    
    override fun deserialize(type: Type, obj: Any): ResourcePath<*> {
        val resourceType = ((type as ParameterizedType).actualTypeArguments[0] as Class<*>).kotlin.objectInstance as ResourceType
        return ResourcePath.of(resourceType, obj.toString())
    }
    
    override fun serialize(item: ResourcePath<*>, typeSupported: Predicate<Class<*>>): Any {
        return item.toString()
    }
    
}