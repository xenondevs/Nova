package xyz.xenondevs.nova.data.resources.builder.task

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.task.font.MovedFontContent
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager

class BarOverlayTask(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    @PackTask(runBefore = ["MovedFontContent#write"])
    private fun requestMovedFonts() {
        if (BossBarOverlayManager.ENABLED) {
            val movedFontContent = builder.getHolder<MovedFontContent>()
            movedFontContent.requestMovedFonts(ResourcePath("minecraft", "default"), 1..19)
            movedFontContent.requestMovedFonts(ResourcePath("minecraft", "uniform"), 1..19)
            movedFontContent.requestMovedFonts(ResourcePath("nova", "bossbar"), 1..19)
        }
    }
    
}