package xyz.xenondevs.nova.data.serialization.configurate

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

internal object ComponentSerializer : ScalarSerializer<Component>(Component::class.java) {
    
    override fun deserialize(type: Type, obj: Any): Component {
        val message = obj.toString().replace("§", "")
        return MiniMessage.miniMessage().deserialize(message)
    }
    
    override fun serialize(item: Component, typeSupported: Predicate<Class<*>>?): Any {
        return MiniMessage.miniMessage().serialize(item)
    }
    
}

