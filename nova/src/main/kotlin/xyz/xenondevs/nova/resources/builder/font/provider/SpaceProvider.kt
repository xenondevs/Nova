package xyz.xenondevs.nova.resources.builder.font.provider

import com.google.gson.JsonObject
import it.unimi.dsi.fastutil.ints.Int2FloatMap
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.getObject

/**
 * Represents a `space` font provider.
 */
class SpaceProvider(val advances: Int2FloatMap) : FontProvider("space") {
    
    override val codePoints: IntSet
        get() = advances.keys
    
    override val charSizes: Int2ObjectMap<FloatArray>
        get() {
            val sizes = Int2ObjectOpenHashMap<FloatArray>()
            for ((codePoint, width) in advances.int2FloatEntrySet()) {
                // width, yMin, yMax
                sizes[codePoint] = floatArrayOf(width, Float.MAX_VALUE, -Float.MAX_VALUE)
            }
            
            return sizes
        }
    
    override fun toJson() = super.toJson().apply {
        add("advances", JsonObject().apply {
            for ((codePoint, width) in advances.int2FloatEntrySet())
                addProperty(Character.toString(codePoint), width)
        })
    }
    
    companion object {
        
        fun of(provider: JsonObject): SpaceProvider {
            val advances = Int2FloatOpenHashMap()
            for ((codePointStr, width) in provider.getObject("advances").entrySet()) {
                val codePoint = codePointStr.codePointAt(0)
                advances[codePoint] = width.asFloat
            }
            
            return SpaceProvider(advances)
        }
        
    }
    
}