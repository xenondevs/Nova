package xyz.xenondevs.nova.resources.builder.task

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.BitmapFontGenerator
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
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
     * Requests a vertically moved font variant of [font] for each offset in [offsets].
     */
    fun requestMovedFonts(font: ResourcePath<ResourceType.Font>, offsets: Iterable<Int>) {
        for (y in offsets) requestMovedFont(font, y)
    }
    
    private fun requestMovedFont(font: ResourcePath<ResourceType.Font>, y: Int) {
        if (font in MOVED_FONT_BLACKLIST)
            return
        
        val request = font to y
        if (requested.add(request))
            queue += request
    }
    
    /**
     * Generates the requested moved font variants from [MovedFontContent] and writes them to [FontContent].
     */
    inner class Write(private val builder: ResourcePackBuilder) : PackTask {
        
        override val runsAfter = setOf(FontContent.LoadAll::class, LanguageContent.LoadAll::class)
        override val runsBefore = setOf(FontContent.Write::class)
        
        private val langContent by builder.getBuildDataLazily<LanguageContent>()
        private val fontContent by builder.getBuildDataLazily<FontContent>()
        
        override suspend fun run() {
            val usedGlyphs = IntOpenHashSet()
            for ([_, t] in langContent.vanillaLangs) for ([_, v] in t) for (c in v.codePoints()) usedGlyphs.add(c)
            for ([_, t] in langContent.customLangs) for ([_, v] in t) for (c in v.codePoints()) usedGlyphs.add(c)
            
            builder.logger.info("Creating moved fonts")
            
            val bitmapFonts = HashMap<ResourcePath<ResourceType.Font>, Font>()
            fun getBitmapFont(id: ResourcePath<ResourceType.Font>): Font {
                return bitmapFonts.getOrPut(id) {
                    val font = fontContent.mergedFonts[id]
                        ?: throw IllegalStateException("Font ${id.asString()} does not exist or is not loaded in FontContent")
                    
                    BitmapFontGenerator(builder, font).generateBitmapFont(if (id.namespace == "minecraft") usedGlyphs else null)
                }
            }
            
            val movedFonts = HashMap<ResourcePath<ResourceType.Font>, Pair<ResourcePath<ResourceType.Font>, Int>>()
            while (queue.isNotEmpty()) {
                val [font, y] = queue.poll()
                
                val fontPath = ResourcePath(ResourceType.Font, font.namespace, font.path + "/$y")
                val movedFont = Font(fontPath)
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
                movedFonts[fontPath] = font to y
            }
            
            ResourceLookups.movedFonts = movedFonts
        }
        
    }
    
    companion object {
        
        /**
         * Gets the source font (or zero font if present) and vertical offset of the moved font under [id], 
         * or null if [id] is not a moved font or if it is a zero font.
         */
        internal fun getSource(id: ResourcePath<ResourceType.Font>): Pair<ResourcePath<ResourceType.Font>, Int>? {
            val [source, y] = ResourceLookups.movedFonts[id]
                ?: return null
            if (y <= 0)
                return null
            val zeroFont = ResourcePath(ResourceType.Font, source.namespace, source.path + "/0")
            if (zeroFont in ResourceLookups.movedFonts)
                return zeroFont to y
            return source to y
        }
        
    }
    
}