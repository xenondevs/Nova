package xyz.xenondevs.nova.serialization.configurate

import org.spongepowered.configurate.serialize.ScalarSerializer
import java.awt.Color
import java.lang.reflect.Type
import java.util.function.Predicate

internal object ColorSerializer : ScalarSerializer<Color>(Color::class.java) {
    
    override fun deserialize(type: Type, obj: Any): Color? {
        return Color.decode(obj.toString())
    }
    
    override fun serialize(item: Color, typeSupported: Predicate<Class<*>?>): Any {
        return String.format("#%06x", item.rgb and 0xFFFFFF)
    }
    
}