package xyz.xenondevs.nova.serialization.configurate

import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatcher
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarOrigin
import xyz.xenondevs.nova.util.data.WildcardUtils
import xyz.xenondevs.nova.util.data.get
import java.lang.reflect.Type
import java.util.*

internal object BarMatcherSerializer : TypeSerializer<BarMatcher> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): BarMatcher {
        val map = node.childrenMap()
        val matcherType = map["type"]?.string
            ?: throw NoSuchElementException("Missing value 'type'")
        
        when (matcherType) {
            "uuid" -> {
                val uuidStr = map["uuid"]?.string
                    ?: throw NoSuchElementException("Missing value 'id'")
                val uuid = runCatching { UUID.fromString(uuidStr) }
                    .getOrElse { throw IllegalArgumentException("Invalid uuid '$uuidStr'") }
                
                return BarMatcher.Id(uuid)
            }
            
            "index" -> {
                val index = map["index"]?.int
                    ?: throw NoSuchElementException("Missing value 'index'")
                
                return BarMatcher.Index(index)
            }
            
            "text" -> {
                val regex = when {
                    "regex" in map -> {
                        val regexStr = map["regex"]?.string
                            ?: throw NoSuchElementException("Missing value 'regex'")
                        runCatching { Regex(regexStr) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid regex '$regexStr'")
                    }
                    
                    "wildcard" in map -> {
                        val wildcard = map["wildcard"]?.string
                            ?: throw NoSuchElementException("Missing value 'wildcard'")
                        WildcardUtils.toRegex(wildcard)
                    }
                    
                    else -> throw NoSuchElementException("Missing value 'regex' or 'wildcard'")
                }
                
                return BarMatcher.Text(regex)
            }
            
            "origin" -> {
                val origin = map["origin"]?.string
                    ?: throw NoSuchElementException("Missing value 'origin'")
                
                if (origin == "minecraft")
                    return BarMatcher.Origin(BarOrigin.Minecraft)
                
                val plugin = Bukkit.getPluginManager().getPlugin(origin)
                    ?: throw IllegalArgumentException("Invalid plugin '$origin'")
                
                return BarMatcher.Origin(BarOrigin.Plugin(plugin))
            }
            
            "overlay" -> {
                val id = map["id"]?.string
                    ?: throw NoSuchElementException("Missing value 'id'")
                val namespacedId = ResourceLocation.parse(id)
                
                return BarMatcher.Origin(BarOrigin.Addon(namespacedId))
            }
            
            else -> throw IllegalArgumentException("Invalid bar matcher type '$type'")
        }
    }
    
    override fun serialize(type: Type, obj: BarMatcher?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }

}

internal object BarMatcherCombinedAnySerializer : TypeSerializer<BarMatcher.CombinedAny> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): BarMatcher.CombinedAny {
        return BarMatcher.CombinedAny(node.get<List<BarMatcher>>()!!)
    }
    
    override fun serialize(type: Type, obj: BarMatcher.CombinedAny?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }

}

internal object BarMatcherCombinedAllSerializer : TypeSerializer<BarMatcher.CombinedAll> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): BarMatcher.CombinedAll {
        return BarMatcher.CombinedAll(node.get<List<BarMatcher>>()!!)
    }
    
    override fun serialize(type: Type, obj: BarMatcher.CombinedAll?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }

}