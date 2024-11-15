package xyz.xenondevs.nova.resources.builder.font.provider

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType

/**
 * Represents a `reference` font provider.
 */
class ReferenceProvider(var id: ResourcePath<ResourceType.Font>) : FontProvider() {
    
    override val codePoints: IntSet
        get() = throw UnsupportedOperationException("Cannot retrieve codePoints from reference provider")
    
    override val charSizes: Int2ObjectMap<FloatArray>
        get() = throw UnsupportedOperationException("Cannot retrieve charSizes from reference provider")
    
    override fun toJson() = JsonObject().apply {
        addProperty("type", "reference")
        addProperty("id", id.toString())
    }
    
    companion object {
        
        fun of(provider: JsonObject) = ReferenceProvider(
            ResourcePath.of(ResourceType.Font, provider.getString("id"))
        )
        
    }
    
}