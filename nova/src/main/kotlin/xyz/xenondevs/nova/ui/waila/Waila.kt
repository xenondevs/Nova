package xyz.xenondevs.nova.ui.waila

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProviderRegistry
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.ui.waila.overlay.WailaImageOverlay
import xyz.xenondevs.nova.ui.waila.overlay.WailaLineOverlay
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos

private val POS_UPDATE_INTERVAL by configReloadable { DEFAULT_CONFIG.getInt("waila.pos_update_interval") }
private val DATA_UPDATE_INTERVAL by configReloadable { DEFAULT_CONFIG.getInt("waila.data_update_interval") }

private val BLACKLISTED_BLOCKS by configReloadable {
    DEFAULT_CONFIG.getStringList("waila.blacklisted_blocks")
        .mapTo(HashSet()) { NamespacedId.of(it, "minecraft") }
}

internal class Waila(val player: Player) {
    
    private var lastPosUpdate: Int = 0
    private var lastDataUpdate: Int = 0
    private var lookingAt: BlockPos? = null
    
    private var active = false
    private val imageOverlay = WailaImageOverlay()
    private val lineOverlays = Array(10, ::WailaLineOverlay).toList()
    
    fun setActive(active: Boolean) {
        if (this.active == active)
            return
        
        this.active = active
        
        if (active) {
            BossBarOverlayManager.registerBackgroundOverlay(player, imageOverlay)
        } else {
            BossBarOverlayManager.unregisterOverlayIf(player) { it == imageOverlay || it in lineOverlays }
        }
    }
    
    fun handleTick() {
        val serverTick = serverTick
        if (serverTick - lastPosUpdate >= POS_UPDATE_INTERVAL) {
            lastPosUpdate = serverTick
            val pos = player.getTargetBlockExact(5)?.pos
            if (pos != lookingAt) {
                lastDataUpdate = serverTick
                update(pos)
            }
        }
        if (serverTick - lastDataUpdate >= DATA_UPDATE_INTERVAL) {
            lastDataUpdate = serverTick
            update(lookingAt)
        }
    }
    
    private fun update(pos: BlockPos?) {
        lookingAt = pos
        setActive(tryUpdate(pos))
    }
    
    private fun tryUpdate(pos: BlockPos?): Boolean {
        if (pos != null) {
            val blockId = pos.block.id
            if (blockId in BLACKLISTED_BLOCKS)
                return false
            
            val info = WailaInfoProviderRegistry.getInfo(player, pos) ?: return false
            val lines = info.lines
            require(lines.size <= 10) { "Waila text can't be longer than 10 lines" }
            
            val icon = Resources.getWailaIconCharOrNull(info.icon) ?: return false
            
            val (beginX, centerX) = imageOverlay.update(icon, lines.size, lines.maxOf { it.width })
            
            BossBarOverlayManager.unregisterOverlays(player, lineOverlays)
            lineOverlays.forEachIndexed { idx, overlay ->
                if (lines.size <= idx) {
                    overlay.clear()
                    return@forEachIndexed
                }
                
                val (text, width, alignment) = lines[idx]
                overlay.text = text
                overlay.textWidth = width
                overlay.centered = alignment == Alignment.CENTERED
                overlay.x = when (alignment) {
                    Alignment.LEFT -> beginX
                    Alignment.CENTERED -> centerX
                    Alignment.FIRST_LINE -> getBeginX(lines, 0, beginX, centerX)
                    Alignment.PREVIOUS_LINE -> getBeginX(lines, idx - 1, beginX, centerX)
                }
                overlay.changed = true
                
                BossBarOverlayManager.registerOverlay(player, overlay)
            }
            
            return true
        }
        
        return false
    }
    
    private fun getBeginX(lines: List<WailaLine>, lineNumber: Int, beginX: Int, centerX: Int): Int {
        var currentLineNumber = lineNumber
        while (true) {
            val line = lines[currentLineNumber]
            
            when (line.alignment) {
                Alignment.LEFT -> return beginX
                Alignment.CENTERED -> return centerX - line.width / 2
                
                Alignment.FIRST_LINE -> currentLineNumber = 0
                Alignment.PREVIOUS_LINE -> currentLineNumber--
            }
        }
    }
    
}