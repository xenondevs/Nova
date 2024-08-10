package xyz.xenondevs.nova.resources.builder.font.provider

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.MutableBitmapProvider
import xyz.xenondevs.nova.resources.builder.font.provider.unihex.UnihexProvider
import java.nio.file.Path

abstract class FontProvider internal constructor() {
    
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
     * Creates a [JsonObject] representation of this [FontProvider].
     */
    abstract fun toJson(): JsonObject
    
    /**
     * Writes additional data to the assets directory, such as bitmaps or unihex files.
     */
    open fun write(assetsDir: Path) = Unit
    
    companion object {
        
        fun fromDisk(provider: JsonObject): FontProvider =
            when (val type = provider.getString("type")) {
                "reference" -> ReferenceProvider.of(provider)
                "space" -> SpaceProvider.of(provider)
                "bitmap" -> MutableBitmapProvider.fromDisk(provider)
                "unihex" -> UnihexProvider.fromDisk(provider)
                else -> throw UnsupportedOperationException("Unsupported font provider type: $type")
            }
        
    }
    
}