package xyz.xenondevs.nova.data.serialization.yaml.serializer

import org.bukkit.Bukkit
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.yaml.YamlSerializer
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarMatcher
import xyz.xenondevs.nova.ui.overlay.bossbar.positioning.BarOrigin
import xyz.xenondevs.nova.util.data.WildcardUtils
import java.util.*

internal object BarMatcherSerializer : YamlSerializer<BarMatcher> {
    
    override fun serialize(value: BarMatcher): MutableMap<String, Any> {
        throw UnsupportedOperationException()
    }
    
    override fun deserialize(map: Map<String, Any>): BarMatcher {
        val type = map["type"] as? String
            ?: throw NoSuchElementException("Missing value 'type'")
        
        when (type) {
            "uuid" -> {
                val uuidStr = map["uuid"] as? String
                    ?: throw NoSuchElementException("Missing value 'id'")
                val uuid = runCatching { UUID.fromString(uuidStr) }
                    .getOrElse { throw IllegalArgumentException("Invalid uuid '$uuidStr'") }
                
                return BarMatcher.Id(uuid)
            }
            
            "index" -> {
                val index = map["index"] as? Int
                    ?: throw NoSuchElementException("Missing value 'index'")
                
                return BarMatcher.Index(index)
            }
            
            "text" -> {
                val regex = when {
                    "regex" in map -> {
                        val regexStr = map["regex"] as String
                        runCatching { Regex(regexStr) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid regex '$regexStr'")
                    }
                    
                    "wildcard" in map -> {
                        val wildcard = map["wildcard"] as String
                        WildcardUtils.toRegex(wildcard)
                    }
                    
                    else -> throw NoSuchElementException("Missing value 'regex' or 'wildcard'")
                }
                
                return BarMatcher.Text(regex)
            }
            
            "origin" -> {
                val origin = map["origin"] as? String
                    ?: throw NoSuchElementException("Missing value 'origin'")
                
                if (origin == "minecraft")
                    return BarMatcher.Origin(BarOrigin.Minecraft)
                
                val plugin = Bukkit.getPluginManager().getPlugin(origin)
                    ?: throw IllegalArgumentException("Invalid plugin '$origin'")
                
                return BarMatcher.Origin(BarOrigin.Plugin(plugin))
            }
            
            "overlay" -> {
                val id = map["id"] as? String
                    ?: throw NoSuchElementException("Missing value 'id'")
                val namespacedId = NamespacedId.of(id)
                
                return BarMatcher.Origin(BarOrigin.Addon(namespacedId))
            }
            
            else -> throw IllegalArgumentException("Invalid bar matcher type '$type'")
        }
    }
    
}