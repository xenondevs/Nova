package xyz.xenondevs.nova.data.resources.builder

import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.CharSizeTable
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.data.resources.builder.task.font.FontContent
import java.util.logging.Level

class CharSizeCalculator internal constructor(builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val fontContent by builder.getHolderLazily<FontContent>()
    
    /**
     * Calculates the char sizes for all fonts that need to be recalculated.
     */
    @PackTask(runAfter = [
        "FontContent#discoverAllFonts", "GuiContent#write", "MoveCharactersContent#write", "MovedFontContent#write",
        "TextureIconContent#write", "WailaContent#write"
    ])
    private fun calculateCharSizes() {
        LOGGER.info("Calculating char sizes...")
        
        val fonts = fontContent.mergedFonts
        val references = fonts.values.associateWith { it.mapReferences(fonts.values) }
        val sortedFonts = CollectionUtils.sortDependencies(fonts.values) { references[it]!! }
        for (font in sortedFonts) {
            try {
                val id = font.id
                val table = CharSizeTable()
                for (provider in font.providers.reversed()) {
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
                LOGGER.log(Level.SEVERE, "Failed to calculate char sizes for font ${font.id}", t)
            }
        }
        
        CharSizes.invalidateCache()
    }
    
}