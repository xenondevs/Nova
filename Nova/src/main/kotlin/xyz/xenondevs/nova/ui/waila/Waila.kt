package xyz.xenondevs.nova.ui.waila

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProviderRegistry
import xyz.xenondevs.nova.ui.waila.overlay.WailaImageOverlay
import xyz.xenondevs.nova.ui.waila.overlay.WailaLineOverlay
import xyz.xenondevs.nova.world.BlockPos

internal class Waila(val player: Player) {
    
    private var lookingAt: BlockPos? = null
    
    private var active = false
    private val imageOverlay = WailaImageOverlay()
    private val lineOverlays = Array(10) { WailaLineOverlay(player, it) }
    
    fun setActive(active: Boolean) {
        if (this.active == active)
            return
        
        this.active = active
        
        val overlays = arrayListOf<BossBarOverlay>(imageOverlay).apply { addAll(lineOverlays) }
        if (active) BossBarOverlayManager.registerOverlays(player, overlays)
        else BossBarOverlayManager.unregisterOverlays(player, overlays)
    }
    
    fun update(pos: BlockPos?) {
        lookingAt = pos
        setActive(tryUpdate(pos))
    }
    
    private fun tryUpdate(pos: BlockPos?): Boolean {
        if (pos != null) {
            val info = WailaInfoProviderRegistry.getInfo(player, pos) ?: return false
            require(info.text.size <= 10) { "Waila text can't be longer than 10 lines" }
            val icon = Resources.getWailaIconCharOrNull(info.icon) ?: return false
            
            val centerX = imageOverlay.update(icon, info.text.size, info.widths.max())
            info.text.forEachIndexed { idx, text ->
                val overlay = lineOverlays[idx]
                overlay.text = text
                overlay.centerX = centerX
                overlay.width = info.widths[idx]
                overlay.changed = true
            }
            
            return true
        }
        
        return false
    }
    
}