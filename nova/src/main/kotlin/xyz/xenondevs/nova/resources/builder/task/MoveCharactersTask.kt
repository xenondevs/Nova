package xyz.xenondevs.nova.resources.builder.task

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.ReferenceProvider
import xyz.xenondevs.nova.resources.builder.font.provider.SpaceProvider
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import kotlin.math.pow

/**
 * Generates horizontal move characters.
 */
class MoveCharactersTask(builder: ResourcePackBuilder) : PackTask {
    
    companion object {
        
        internal const val SIZE = 16
        internal const val EXP_SHIFT = 2
        internal val PRECISION = 2.0.pow(EXP_SHIFT)
        
    }
    
    override val stage = BuildStage.PRE_WORLD
    override val runsAfter = setOf(FontContent.DiscoverAllFonts::class)
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class)
    
    private val fontContent by builder.getBuildDataLazily<FontContent>()
    
    override suspend fun run() {
        val mergedFonts = fontContent.mergedFonts
        
        val codePoints = IntOpenHashSet()
        codePoints.addAll(mergedFonts[Font.DEFAULT]!!.getCodePoints(mergedFonts.values))
        codePoints.addAll(mergedFonts[Font.UNIFORM]!!.getCodePoints(mergedFonts.values))
        
        val offset = Font.PRIVATE_USE_AREA.start
        
        val advances = Int2FloatOpenHashMap()
        // -.25, -.5, ..., -8192
        for (i in 0..<SIZE) advances[offset + i] = -2.0.pow(i - EXP_SHIFT).toFloat()
        // .25, .5, ..., 8192
        for (i in 0..<SIZE) advances[offset + SIZE + i] = 2.0.pow(i - EXP_SHIFT).toFloat()
        
        val moveFontId = ResourcePath(ResourceType.Font, "nova", "move")
        val spaceFont = Font(moveFontId, listOf(SpaceProvider(advances)))
        fontContent += spaceFont
        
        val spaceFontReference = ReferenceProvider(moveFontId)
        fontContent.getOrCreate(Font.DEFAULT).addFirst(spaceFontReference)
        fontContent.getOrCreate(Font.UNIFORM).addFirst(spaceFontReference)
        
        // update lookup
        ResourceLookups.MOVE_CHARACTERS_OFFSET = offset
    }
    
}