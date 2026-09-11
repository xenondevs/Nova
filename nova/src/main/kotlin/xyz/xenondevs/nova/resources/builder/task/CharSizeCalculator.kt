package xyz.xenondevs.nova.resources.builder.task

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.CharSizeTable
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider

private val SETTINGS: Set<String> by combinedProvider(
    MAIN_CONFIG.entry<Boolean>(arrayOf("resource_pack", "generation", "force_uniform_font"), arrayOf("resource_pack", "force_uniform_font")),
    MAIN_CONFIG.entry<Boolean>(arrayOf("resource_pack", "generation", "japanese_glyph_variants"), arrayOf("resource_pack", "japanese_glyph_variants"))
) { uniform, jp ->
    buildSet {
        if (uniform) add("uniform")
        if (jp) add("jp")
    }
}

/**
 * Calculates char sizes for all fonts.
 */
class CharSizeCalculator(private val builder: ResourcePackBuilder) : PackTask {
    
    override val runsAfter = setOf(
        FontContent.LoadAll::class, GuiTextureTask::class, MoveCharactersTask::class,
        MovedFontContent.Write::class, TextureIconContent.Write::class, WailaTask::class
    )
    
    private val fontContent by builder.getBuildDataLazily<FontContent>()
    private val movedFontContent by builder.getBuildDataLazily<MovedFontContent>()
    
    override suspend fun run() {
        builder.logger.info("Calculating char sizes...")
        
        coroutineScope {
            for (font in fontContent.mergedFonts.values) {
                launch(Dispatchers.Default) {
                    try {
                        val table = calculateTable(font)
                        withContext(Dispatchers.IO) {
                            CharSizes.storeTable(font.id, table)
                        }
                    } catch (t: CancellationException) {
                        throw t
                    } catch (t: Throwable) {
                        builder.logger.error("Failed to calculate char sizes for font ${font.id.asString()}", t)
                    }
                }
            }
        }
        
        CharSizes.invalidateCache()
    }
    
    private fun calculateTable(font: Font): CharSizeTable {
        movedFontContent.getSource(font.id)?.let { [sourceFont, offset] ->
            return CharSizeTable.moved(sourceFont, offset)
        }
        
        val tables = ArrayList<CharSizeTable>()
        var directMetrics: Int2ObjectOpenHashMap<FloatArray>? = null
        
        fun flushDirectMetrics() {
            val metrics = directMetrics ?: return
            tables += CharSizeTable.direct(metrics)
            directMetrics = null
        }
        
        for (provider in font.providers) {
            if (provider.filter.any { [filterKey, filterValue] -> filterValue != filterKey in SETTINGS })
                continue
            
            if (provider is ReferenceProvider) {
                flushDirectMetrics()
                tables += CharSizeTable.reference(provider.id)
            } else {
                val metrics = directMetrics ?: Int2ObjectOpenHashMap<FloatArray>().also { directMetrics = it }
                for (entry in provider.charSizes.int2ObjectEntrySet())
                    metrics.putIfAbsent(entry.intKey, entry.value)
            }
        }
        
        flushDirectMetrics()
        return CharSizeTable.composite(tables)
    }
    
}
