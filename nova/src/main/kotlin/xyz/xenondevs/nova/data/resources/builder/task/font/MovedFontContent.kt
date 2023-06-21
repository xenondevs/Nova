package xyz.xenondevs.nova.data.resources.builder.task.font

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.BitmapFontGenerator
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.font.Font
import xyz.xenondevs.nova.data.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.data.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import java.util.*

private val MOVED_FONT_BLACKLIST: Set<ResourcePath> by configReloadable {
    DEFAULT_CONFIG.getStringList("resource_pack.generation.font.moved_font_blacklist")
        .mapTo(HashSet(), ResourcePath::of)
}

class MovedFontContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val fontContent by lazy { builder.getHolder<FontContent>() }
    
    /**
     * A cache for generated bitmap fonts
     */
    private val bitmapFonts = HashMap<ResourcePath, Font>()
    
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
    
    @PackTask(runAfter = ["FontContent#discoverAllFonts"], runBefore = ["FontContent#write"])
    private fun write() {
        LOGGER.info("Creating moved fonts")
        
        while (queue.isNotEmpty()) {
            val (font, y) = queue.poll()
            
            val movedFont = Font(ResourcePath(font.namespace, font.path + "/$y"))
            val bitmapFont = getBitmapFont(font)
            
            for (provider in bitmapFont.providers) {
                movedFont += when (provider) {
                    is BitmapProvider<*> -> BitmapProvider.reference(provider, provider.ascent - y)
                    
                    is ReferenceProvider -> {
                        val id = provider.id
                        requestMovedFont(id, y)
                        ReferenceProvider(ResourcePath(id.namespace, id.path + "/$y"))
                    }
                    
                    else -> provider
                }
            }
            
            fontContent += movedFont
        }
    }
    
    private fun getBitmapFont(id: ResourcePath): Font {
        return bitmapFonts.getOrPut(id) {
            val font = fontContent.mergedFonts[id]
                ?: throw IllegalStateException("Font $id does not exist or is not loaded in FontContent")
            
            BitmapFontGenerator(font).generateBitmapFont()
        }
    }
    
}