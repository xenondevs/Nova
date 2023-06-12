package xyz.xenondevs.nova.data.resources.builder.content.font

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.BitmapFontGenerator
import xyz.xenondevs.nova.data.resources.builder.CharSizeCalculator
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import xyz.xenondevs.nova.data.resources.builder.content.PackContentType
import java.util.*
import kotlin.io.path.createDirectories

private val MOVED_FONT_BLACKLIST: Set<ResourcePath> by configReloadable {
    DEFAULT_CONFIG.getStringList("resource_pack.generation.font.moved_font_blacklist")
        .mapTo(HashSet(), ResourcePath::of)
}

class MovedFontContent private constructor() : PackContent {
    
    companion object : PackContentType<MovedFontContent> {
        override val runBefore = setOf(CharSizeCalculator)
        override fun create(builder: ResourcePackBuilder) = MovedFontContent()
    }
    
    /**
     * A cache for generated bitmap fonts
     */
    private val bitmapFonts = HashMap<ResourcePath, JsonObject>()
    
    /**
     * A set keeping track of requested font variants.
     */
    private val requested = HashSet<Pair<ResourcePath, Int>>()
    
    /**
     * A queue of font variants that need to be generated.
     */
    private val queue = LinkedList<Pair<ResourcePath, Int>>()
    
    fun requestMovedFonts(font: ResourcePath, range: IntRange) {
        for (y in range) requestMovedFont(font, y)
    }
    
    fun requestMovedFonts(font: ResourcePath, range: IntProgression) {
        for (y in range) requestMovedFont(font, y)
    }
    
    private fun requestMovedFont(font: ResourcePath, y: Int) {
        if (font in MOVED_FONT_BLACKLIST)
            return
        
        val pair = font to y
        if (pair in requested)
            return
        
        requested += pair
        queue += pair
    }
    
    override fun includePack(pack: AssetPack) {
        pack.movedFontsIndex?.forEach { (font, variants) -> variants.forEach { y -> requestMovedFont(font, y) } }
    }
    
    override fun write() {
        LOGGER.info("Creating moved fonts")
        while (queue.isNotEmpty()) {
            val (font, y) = queue.poll()
            val bitmapFont = getBitmapFont(font).deepCopy()
            val providers = bitmapFont.getAsJsonArray("providers")
            providers.forEach { provider ->
                provider as JsonObject
                
                when (provider.getString("type")) {
                    "reference" -> {
                        val id = ResourcePath.of(provider.getString("id"))
                        provider.addProperty("id", "$id/$y")
                        requestMovedFont(id, y)
                    }
                    
                    "bitmap" -> {
                        val currentAscent = provider.getIntOrNull("ascent") ?: 0
                        provider.addProperty("ascent", currentAscent - y)
                    }
                }
            }
            
            val file = ResourcePackBuilder.ASSETS_DIR.resolve("${font.namespace}/font/${font.path}/$y.json")
            file.parent.createDirectories()
            bitmapFont.writeToFile(file)
        }
    }
    
    private fun getBitmapFont(font: ResourcePath): JsonObject =
        bitmapFonts.getOrPut(font) { BitmapFontGenerator(font).generateBitmapFont() }
    
}