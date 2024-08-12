package xyz.xenondevs.nova.resources.builder

import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.util.data.WildcardUtils

class ResourceFilter(
    val stage: Stage,
    val type: Type,
    val filter: Regex,
    val directory: String? = null
) {
    
    constructor(stage: Stage, type: Type, filterWildcard: String, directory: String? = null) :
        this(stage, type, WildcardUtils.toRegex(filterWildcard), directory)
    
    fun allows(path: String): Boolean =
        when (type) {
            Type.WHITELIST -> (directory != null && !path.startsWith(directory)) || filter.matches(path)
            Type.BLACKLIST -> (directory != null && !path.startsWith(directory)) || !filter.matches(path)
        }
    
    companion object {
        
        internal fun of(cfg: ConfigurationSection): ResourceFilter {
            val stage = cfg.getString("stage")?.let { Stage.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("Missing property 'stage' in content filter")
            val type = cfg.getString("type")?.let { Type.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("Missing property 'type' in content filter")
            val patternType = cfg.getString("pattern_type")
                ?: throw IllegalArgumentException("Missing property 'pattern_type' in content filter")
            val filter = cfg.getString("filter")
                ?: throw IllegalArgumentException("Missing property 'filter' in content filter")
            
            val directory = cfg.getString("directory")
            
            val regex = when (patternType) {
                "wildcard" -> WildcardUtils.toRegex(filter)
                "regex" -> Regex(filter)
                else -> throw UnsupportedOperationException("Unsupported pattern type: $patternType")
            }
            
            return ResourceFilter(stage, type, regex, directory)
        }
        
    }
    
    enum class Stage {
        ASSET_PACK,
        RESOURCE_PACK
    }
    
    enum class Type {
        WHITELIST,
        BLACKLIST
    }
    
    enum class PatternType {
        WILDCARD,
        REGEX
    }
    
}
