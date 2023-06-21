package xyz.xenondevs.nova.data.resources.builder.task.font

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.font.Font
import xyz.xenondevs.nova.data.resources.builder.font.provider.SpaceProvider
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.toIntArray
import kotlin.math.pow

class MoveCharactersContent(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    companion object {
        
        private val DEFAULT_FONT = ResourcePath("minecraft", "default")
        
        internal const val SIZE = 16
        internal const val EXP_SHIFT = 2
        internal val PRECISION = 2.0.pow(EXP_SHIFT)
        
    }
    
    @PackTask(runAfter = ["FontContent#discoverAllFonts"], runBefore = ["FontContent#write"])
    private fun write() {
        val fontContent = builder.getHolder<FontContent>()
        val mergedFonts = fontContent.mergedFonts
        val font = mergedFonts[DEFAULT_FONT]!!
        val occupied = font.getCodePoints(mergedFonts.values)
        val range = Font.findFirstUnoccupiedRange(occupied, 0xE000..0xF8FF, SIZE * 2).toIntArray()
        
        val advances = Int2FloatOpenHashMap()
        // -.25, -.5, ..., -8192
        for (i in 0 until SIZE) advances[range[i]] = -2.0.pow(i - EXP_SHIFT).toFloat()
        // .25, .5, ..., 8192
        for (i in 0 until SIZE) advances[range[i + SIZE]] = 2.0.pow(i - EXP_SHIFT).toFloat()
        
        fontContent.getOrCreate(DEFAULT_FONT).addFirst(SpaceProvider(advances))
        
        // update lookup
        ResourceLookups.MOVE_CHARACTERS_OFFSET = range[0]
    }
    
}