package xyz.xenondevs.nova.data.resources.builder.content.font

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.getIntOrNull
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.BitmapFontGenerator
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder.BuildingStage
import xyz.xenondevs.nova.data.resources.builder.content.PackContent
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val DEFAULT_FONT = ResourcePath("minecraft", "default")

private val FORCE_UNICODE_FONT: Boolean by configReloadable {
    DEFAULT_CONFIG.getBoolean("resource_pack.generation.font.force_unicode_font")
}

private val MOVED_FONT_BLACKLIST: Set<ResourcePath> by configReloadable {
    DEFAULT_CONFIG.getStringList("resource_pack.generation.font.moved_font_blacklist")
        .mapTo(HashSet(), ResourcePath::of)
}

internal class MovedFontContent : PackContent {
    
    override val stage = BuildingStage.POST_WORLD
    private val movingRequests = HashMap<ResourcePath, HashSet<Int>>()
    
    fun requestMovedFonts(font: ResourcePath, range: IntRange) {
        movingRequests.getOrPut(font) { HashSet() } += range
    }
    
    fun requestMovedFonts(font: ResourcePath, range: IntProgression) {
        movingRequests.getOrPut(font) { HashSet() } += range
    }
    
    override fun includePack(pack: AssetPack) {
        pack.movedFontsIndex?.forEach { (font, variants) -> movingRequests.getOrPut(font, ::HashSet) += variants }
    }
    
    override fun write() {
        // add all fonts from other namespaces (for boss bar overlay, because we don't know which fonts are used in boss bars)
        ResourcePackBuilder.ASSETS_DIR.listDirectoryEntries()
            .filter { it.isDirectory() && it.name != "nova" && it.name != "minecraft" }
            .forEach {
                val namespace = it.name
                val fontDir = it.resolve("font")
                fontDir.forEachDirectoryEntry { fontFile ->
                    if (fontFile.isRegularFile() && fontFile.extension == "json") {
                        requestMovedFonts(ResourcePath(namespace, fontFile.nameWithoutExtension), 1..19)
                    }
                }
            }
        
        // create moved fonts
        LOGGER.info("Creating moved fonts")
        movingRequests.forEach { (font, variants) ->
            if (font in MOVED_FONT_BLACKLIST)
                return@forEach
            
            val bitmapFont = BitmapFontGenerator(font, font == DEFAULT_FONT && FORCE_UNICODE_FONT).generateBitmapFont()
            
            variants.forEach { y ->
                val fontCopy = bitmapFont.deepCopy()
                val providers = fontCopy.getAsJsonArray("providers")
                providers.forEach { provider ->
                    require(provider is JsonObject)
                    val currentAscent = provider.getIntOrNull("ascent") ?: 0
                    provider.addProperty("ascent", currentAscent - y)
                }
                
                val file = ResourcePackBuilder.ASSETS_DIR.resolve("${font.namespace}/font/${font.path}/$y.json")
                file.parent.createDirectories()
                fontCopy.writeToFile(file)
            }
        }
    }
    
}