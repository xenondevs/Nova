package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.BitmapFontGenerator
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import java.util.*

private val MOVED_FONT_BLACKLIST by MAIN_CONFIG.entry<Set<ResourcePath<ResourceType.Font>>>("resource_pack", "generation", "font", "moved_font_blacklist")

/**
 * Allows requesting vertically moved fonts.
 */
class MovedFontContent : PackBuildData {
    
    /**
     * A set keeping track of requested font variants.
     */
    private val requested = HashSet<Pair<ResourcePath<ResourceType.Font>, Int>>()
    
    /**
     * A queue of font variants that need to be generated.
     */
    private val queue = LinkedList<Pair<ResourcePath<ResourceType.Font>, Int>>()
    
    /**
     * Requests a vertically moved font variant of [font] for each offset in [range].
     */
    fun requestMovedFonts(font: ResourcePath<ResourceType.Font>, range: IntRange) {
        for (y in range) requestMovedFont(font, y)
    }
    
    /**
     * Requests a vertically moved font variant of [font] for each offset in [range].
     */
    fun requestMovedFonts(font: ResourcePath<ResourceType.Font>, range: IntProgression) {
        for (y in range) requestMovedFont(font, y)
    }
    
    private fun requestMovedFont(font: ResourcePath<ResourceType.Font>, y: Int) {
        if (font in MOVED_FONT_BLACKLIST)
            return
        
        val pair = font to y
        if (pair in requested)
            return
        
        requested += pair
        queue += pair
    }
    
    /**
     * Generates the requested moved font variants from [MovedFontContent] and writes them to [FontContent].
     */
    inner class Write(private val builder: ResourcePackBuilder) : PackTask {
        
        override val runsAfter = setOf(FontContent.DiscoverAllFonts::class)
        override val runsBefore = setOf(FontContent.Write::class)
        
        private val fontContent by builder.getBuildDataLazily<FontContent>()
        
        override suspend fun run() {
            builder.logger.info("Creating moved fonts")
            
            val bitmapFonts = HashMap<ResourcePath<ResourceType.Font>, Font>()
            fun getBitmapFont(id: ResourcePath<ResourceType.Font>): Font {
                return bitmapFonts.getOrPut(id) {
                    val font = fontContent.mergedFonts[id]
                        ?: throw IllegalStateException("Font $id does not exist or is not loaded in FontContent")
                    
                    BitmapFontGenerator(builder, font).generateBitmapFont()
                }
            }
            
            while (queue.isNotEmpty()) {
                val (font, y) = queue.poll()
                
                val movedFont = Font(ResourcePath(ResourceType.Font, font.namespace, font.path + "/$y"))
                val bitmapFont = getBitmapFont(font)
                
                for (provider in bitmapFont.providers) {
                    movedFont += when (provider) {
                        is BitmapProvider<*> -> BitmapProvider.reference(provider, provider.ascent - y)
                        
                        is ReferenceProvider -> {
                            val id = provider.id
                            requestMovedFont(id, y)
                            ReferenceProvider(ResourcePath(ResourceType.Font, id.namespace, id.path + "/$y")).apply {
                                filter.putAll(provider.filter)
                            }
                        }
                        
                        else -> provider
                    }
                }
                
                fontContent += movedFont
            }
        }
        
    }
    
}