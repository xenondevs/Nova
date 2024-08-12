package xyz.xenondevs.nova.resources.builder.task.font

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.SpaceProvider
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.toIntArray
import kotlin.math.pow

class MoveCharactersContent(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    companion object {
        
        internal const val SIZE = 16
        internal const val EXP_SHIFT = 2
        internal val PRECISION = 2.0.pow(EXP_SHIFT)
        
    }
    
    @PackTask(runAfter = ["FontContent#discoverAllFonts"], runBefore = ["MovedFontContent#write", "FontContent#write"])
    private fun write() {
        val fontContent = builder.getHolder<FontContent>()
        val mergedFonts = fontContent.mergedFonts
        
        val codePoints = IntOpenHashSet()
        codePoints.addAll(mergedFonts[Font.DEFAULT]!!.getCodePoints(mergedFonts.values))
        codePoints.addAll(mergedFonts[Font.UNIFORM]!!.getCodePoints(mergedFonts.values))
        
        val range = Font.findFirstUnoccupiedRange(codePoints, Font.PRIVATE_USE_AREA, SIZE * 2).toIntArray()
        
        val advances = Int2FloatOpenHashMap()
        // -.25, -.5, ..., -8192
        for (i in 0..<SIZE) advances[range[i]] = -2.0.pow(i - EXP_SHIFT).toFloat()
        // .25, .5, ..., 8192
        for (i in 0..<SIZE) advances[range[i + SIZE]] = 2.0.pow(i - EXP_SHIFT).toFloat()
        
        val moveFontId = ResourcePath("nova", "move")
        val spaceFont = Font(moveFontId, listOf(SpaceProvider(advances)))
        fontContent += spaceFont
        
        val spaceFontReference = ReferenceProvider(moveFontId)
        fontContent.getOrCreate(Font.DEFAULT).addFirst(spaceFontReference)
        fontContent.getOrCreate(Font.UNIFORM).addFirst(spaceFontReference)
        
        // update lookup
        ResourceLookups.MOVE_CHARACTERS_OFFSET = range[0]
    }
    
}