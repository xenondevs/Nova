package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Bukkit
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarOrigin

internal object BarOriginSerializer : KSerializer<BarOrigin> {
    
    override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.BarOrigin", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: BarOrigin) {
        when (value) {
            is BarOrigin.Minecraft -> Minecraft.serialize(encoder, value)
            is BarOrigin.Plugin -> Plugin.serialize(encoder, value)
        }
    }
    
    override fun deserialize(decoder: Decoder): BarOrigin {
        val str = decoder.decodeString()
        if (str == "minecraft")
            return BarOrigin.Minecraft
        
        val plugin = Bukkit.getPluginManager().getPlugin(str)
            ?: throw SerializationException("Unknown plugin: $str")
        
        return BarOrigin.Plugin(plugin)
    }
    
    object Minecraft : KSerializer<BarOrigin.Minecraft> {
        
        override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.BarOrigin.Minecraft", PrimitiveKind.STRING)
        
        override fun serialize(encoder: Encoder, value: BarOrigin.Minecraft) {
            encoder.encodeString("minecraft")
        }
        
        override fun deserialize(decoder: Decoder): BarOrigin.Minecraft {
            val str = decoder.decodeString()
            if (str != "minecraft")
                throw SerializationException("Expected 'minecraft', got '$str'")
            return BarOrigin.Minecraft
        }
        
    }
    
    object Plugin : KSerializer<BarOrigin.Plugin> {
        
        override val descriptor = PrimitiveSerialDescriptor("xyz.xenondevs.nova.BarOrigin.Plugin", PrimitiveKind.STRING)
        
        override fun serialize(encoder: Encoder, value: BarOrigin.Plugin) {
            encoder.encodeString(value.plugin.name)
        }
        
        override fun deserialize(decoder: Decoder): BarOrigin.Plugin {
            val name = decoder.decodeString()
            val plugin = Bukkit.getPluginManager().getPlugin(name)
                ?: throw IllegalArgumentException("Unknown plugin: $name")
            return BarOrigin.Plugin(plugin)
        }
        
    }
    
}
