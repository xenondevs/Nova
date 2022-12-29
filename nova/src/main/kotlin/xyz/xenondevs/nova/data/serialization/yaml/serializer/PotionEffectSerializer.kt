package xyz.xenondevs.nova.data.serialization.yaml.serializer

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.yaml.YamlSerializer

internal object PotionEffectSerializer : YamlSerializer<PotionEffect> {
    
    override fun serialize(value: PotionEffect): MutableMap<String, Any> {
        return hashMapOf(
            "type" to value.type.key.toString(),
            "duration" to value.duration,
            "amplifier" to value.amplifier,
            "ambient" to value.isAmbient,
            "particles" to value.hasParticles(),
            "icon" to value.hasIcon()
        )
    }
    
    override fun deserialize(map: Map<String, Any>): PotionEffect {
        val typeStr = map["type"] as? String
            ?: throw NoSuchElementException("Missing value 'type'")
        
        val type = PotionEffectType.getByKey(
            NamespacedId.of(map["type"] as String, "minecraft").toNamespacedKey()
        ) ?: throw IllegalArgumentException("Invalid potion effect type '$typeStr'")
        
        return PotionEffect(
            type,
            map["duration"] as? Int
                ?: throw NoSuchElementException("Missing value 'duration'"),
            map["amplifier"] as? Int
                ?: throw NoSuchElementException("Missing value 'amplifier'"),
            map["ambient"] as? Boolean ?: true,
            map["particles"] as? Boolean ?: true,
            map["icon"] as? Boolean ?: true
        )
    }
    
}