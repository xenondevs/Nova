package xyz.xenondevs.nova.data.serialization.configurate

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.nova.data.resources.builder.ResourceFilter
import xyz.xenondevs.nova.util.data.WildcardUtils
import xyz.xenondevs.nova.util.data.get
import java.lang.reflect.Type

internal object ResourceFilterSerializer : TypeSerializer<ResourceFilter> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): ResourceFilter {
        val filterStage = node.node("stage").get<ResourceFilter.Stage>()
            ?: throw NoSuchElementException("Missing property 'stage' in content filter")
        val filterType = node.node("type").get<ResourceFilter.Type>()
            ?: throw NoSuchElementException("Missing property 'type' in content filter")
        val patternType = node.node("pattern_type").get<ResourceFilter.PatternType>()
            ?: throw NoSuchElementException("Missing property 'pattern_type' in content filter")
        val filter = node.node("filter").string
            ?: throw NoSuchElementException("Missing property 'filter' in content filter")
        
        val directory = node.node("directory").string
        
        val regex = when (patternType) {
            ResourceFilter.PatternType.WILDCARD -> WildcardUtils.toRegex(filter)
            ResourceFilter.PatternType.REGEX -> Regex(filter)
        }
        
        return ResourceFilter(filterStage, filterType, regex, directory)
    }
    
    override fun serialize(type: Type?, obj: ResourceFilter?, node: ConfigurationNode?) {
        throw UnsupportedOperationException()
    }
    
}
