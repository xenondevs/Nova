package xyz.xenondevs.nova.data.resources.builder.task

import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.task.font.MovedFontContent

class BarOverlayTask(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    @PackTask(stage = BuildStage.POST_EXTRACT_ASSETS)
    private fun requestMovedFonts() {
        if (DEFAULT_CONFIG.getBoolean("overlay.bossbar.enabled")) {
            val fontContent = builder.getHolder<MovedFontContent>()
            fontContent.requestMovedFonts(ResourcePath("minecraft", "default"), 1..19)
            fontContent.requestMovedFonts(ResourcePath("minecraft", "uniform"), 1..19)
            fontContent.requestMovedFonts(ResourcePath("nova", "bossbar"), 1..19)
        }
    }
    
}