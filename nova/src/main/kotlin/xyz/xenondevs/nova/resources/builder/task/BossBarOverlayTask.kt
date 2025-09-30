package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager

/**
 * Generates additional resources required for the boss bar overlay.
 */
class BossBarOverlayTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val runsBefore = setOf(MovedFontContent.Write::class)
    
    override suspend fun run() {
        if (BossBarOverlayManager.ENABLED) {
            val movedFontContent = builder.getBuildData<MovedFontContent>()
            movedFontContent.requestMovedFonts(ResourcePath(ResourceType.Font, "minecraft", "default"), 1..19)
            movedFontContent.requestMovedFonts(ResourcePath(ResourceType.Font, "minecraft", "uniform"), 1..19)
            movedFontContent.requestMovedFonts(ResourcePath(ResourceType.Font, "nova", "bossbar"), 1..19)
        }
    }
    
}