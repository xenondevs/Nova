package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

internal object AttributeModifierDisplaySerializer : ScalarSerializer<AttributeModifierDisplay>(AttributeModifierDisplay::class.java) {
    
    override fun deserialize(type: Type, obj: Any): AttributeModifierDisplay? {
        val str = obj.toString()
        return when (str) {
            "default" -> AttributeModifierDisplay.reset()
            "hidden" -> AttributeModifierDisplay.hidden()
            else -> AttributeModifierDisplay.override(MiniMessage.miniMessage().deserialize(str))
        }
    }
    
    override fun serialize(item: AttributeModifierDisplay, typeSupported: Predicate<Class<*>?>?): Any? {
        return when (item) {
            is AttributeModifierDisplay.Default -> "default"
            is AttributeModifierDisplay.Hidden -> "hidden"
            is AttributeModifierDisplay.OverrideText -> MiniMessage.miniMessage().serialize(item.text())
            else -> throw UnsupportedOperationException()
        }
    }
    
}