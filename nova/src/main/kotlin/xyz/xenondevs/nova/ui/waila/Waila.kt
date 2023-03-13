package xyz.xenondevs.nova.ui.waila

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProviderRegistry
import xyz.xenondevs.nova.ui.waila.overlay.WailaOverlayCompound
import xyz.xenondevs.nova.util.data.WildcardUtils
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos

private val POS_UPDATE_INTERVAL by configReloadable { DEFAULT_CONFIG.getInt("waila.pos_update_interval") }
private val DATA_UPDATE_INTERVAL by configReloadable { DEFAULT_CONFIG.getInt("waila.data_update_interval") }

private val BLACKLISTED_BLOCKS by configReloadable {
    DEFAULT_CONFIG.getStringList("waila.blacklisted_blocks").map {
        val parts = it.split(':')
        if (parts.size == 1) {
            Regex("minecraft") to WildcardUtils.toRegex(it)
        } else {
            WildcardUtils.toRegex(parts[0]) to WildcardUtils.toRegex(parts[1])
        }
    }
}

internal class Waila(val player: Player) {
    
    private var lastPosUpdate: Int = 0
    private var lastDataUpdate: Int = 0
    private var lookingAt: BlockPos? = null
    
    private var active = false
    private val overlay = WailaOverlayCompound(player)
    
    private var prevInfo: WailaInfo? = null
    
    fun setActive(active: Boolean) {
        if (this.active == active)
            return
        
        this.active = active
        
        if (active) {
            BossBarOverlayManager.registerOverlay(player, overlay)
        } else {
            BossBarOverlayManager.unregisterOverlay(player, overlay)
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
            if (isBlacklisted(blockId))
                return false
            
            val info = WailaInfoProviderRegistry.getInfo(player, pos)
                ?: return false
            
            if (info != prevInfo) {
                prevInfo = info
                overlay.update(info.icon, info.lines)
            }
            
            return true
        }
        
        return false
    }
    
    private fun isBlacklisted(id: NamespacedId) =
        BLACKLISTED_BLOCKS.any { (namespaceRegex, nameRegex) ->
            namespaceRegex.matches(id.namespace) && nameRegex.matches(id.name)
        }
    
}