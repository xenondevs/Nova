package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.CharSizeTable
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
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
class CharSizeCalculator internal constructor(private val builder: ResourcePackBuilder) : PackTask {
    
    override val runAfter = setOf(
        FontContent.DiscoverAllFonts::class, GuiTextureTask::class, MoveCharactersTask::class,
        MovedFontContent.Write::class, TextureIconContent.Write::class, WailaTask::class
    )
    
    private val fontContent by builder.getBuildDataLazily<FontContent>()
    
    override suspend fun run() {
        builder.logger.info("Calculating char sizes...")
        
        val fonts = fontContent.mergedFonts
        val references = fonts.values.associateWith { it.mapReferences(fonts.values) }
        val sortedFonts = CollectionUtils.sortDependencies(fonts.values) { references[it]!! }
        for (font in sortedFonts) {
            try {
                val id = font.id
                val table = CharSizeTable()
                for (provider in font.providers.reversed()) {
                    // skip providers that have mismatching filters
                    if (provider.filter.any { (filterKey, filterValue) -> filterValue != filterKey in SETTINGS })
                        continue
                    
                    if (provider is ReferenceProvider) {
                        val referencedTable = CharSizes.getTable(provider.id)
                            ?: throw IllegalStateException("Referenced font ${provider.id} has no char size table")
                        table.merge(referencedTable)
                    } else {
                        table.merge(provider.charSizes)
                    }
                }
                
                CharSizes.storeTable(id, table)
            } catch (t: Throwable) {
                builder.logger.error("Failed to calculate char sizes for font ${font.id}", t)
            }
        }
        
        CharSizes.invalidateCache()
    }
    
}