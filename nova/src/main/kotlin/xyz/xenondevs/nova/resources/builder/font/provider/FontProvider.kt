package xyz.xenondevs.nova.resources.builder.font.provider

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.MutableBitmapProvider
import xyz.xenondevs.nova.resources.builder.font.provider.unihex.UnihexProvider
import xyz.xenondevs.nova.serialization.json.addSerialized
import xyz.xenondevs.nova.serialization.json.getDeserialized

abstract class FontProvider internal constructor(private val type: String) {
    
    /**
     * The code points that this [FontProvider] supplies.
     *
     * # DO NOT MUTATE
     */
    abstract val codePoints: IntSet
    
    /**
     * The sizes of the supplied characters of this [FontProvider].
     *
     * Implementations of this should be lazy, meaning that any modifications to the font after retrieving this map
     * will not be present. This is why this property is internal.
     *
     * Format: `size -> [width, height, ascent, yMin, yMax]`
     *
     * # DO NOT MUTATE
     */
    internal abstract val charSizes: Int2ObjectMap<FloatArray>
    
    /**
     * Filter options that determine when this font provider should be used.
     * 
     * * `uniform`: Optional. The value that "Force Uniform" must be for this font provider to be enabled.
     * * `jp`: Optional. The value that "Japanese Glyph Variants" must be for this font provider to be enabled.
     */
    open val filter: MutableMap<String, Boolean> = HashMap()
    
    /**
     * Creates a [JsonObject] representation of this [FontProvider].
     */
    open fun toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("type", type)
            if (filter.isNotEmpty()) addSerialized("filter", filter)
        }
    }
    
    /**
     * Writes additional data to the assets directory, such as bitmaps or unihex files.
     */
    open fun write(builder: ResourcePackBuilder) = Unit
    
    companion object {
        
        fun fromDisk(builder: ResourcePackBuilder, provider: JsonObject): FontProvider {
            val fontProvider = when (val type = provider.getString("type")) {
                "reference" -> ReferenceProvider.of(provider)
                "space" -> SpaceProvider.of(provider)
                "bitmap" -> MutableBitmapProvider.fromDisk(builder, provider)
                "unihex" -> UnihexProvider.fromDisk(builder, provider)
                else -> throw UnsupportedOperationException("Unsupported font provider type: $type")
            }
            
            if (provider.has("filter")) {
                fontProvider.filter.putAll(provider.getDeserialized<Map<String, Boolean>>("filter"))
            }
            
            return fontProvider
        }
        
    }
    
}