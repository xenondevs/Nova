package xyz.xenondevs.nova.ui.waila

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries.WAILA_INFO_PROVIDER
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.ui.waila.overlay.WailaOverlayCompound
import xyz.xenondevs.nova.util.data.WildcardUtils
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import kotlin.math.roundToInt

private val POS_UPDATE_INTERVAL by MAIN_CONFIG.entry<Int>("waila", "pos_update_interval")
private val DATA_UPDATE_INTERVAL by MAIN_CONFIG.entry<Int>("waila", "data_update_interval")
private val BLACKLISTED_BLOCKS by MAIN_CONFIG.entry<List<String>>("waila", "blacklisted_blocks").mapEach {
    val parts = it.split(':')
    if (parts.size == 1) {
        Regex("minecraft") to WildcardUtils.toRegex(it)
    } else {
        WildcardUtils.toRegex(parts[0]) to WildcardUtils.toRegex(parts[1])
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
            val pos = player.getTargetBlockExact(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)!!.value.roundToInt())?.pos
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
            
            val info = getInfo(player, pos)
                ?: return false
            
            if (info != prevInfo) {
                prevInfo = info
                overlay.update(info.icon, info.lines)
            }
            
            return true
        }
        
        return false
    }
    
    private fun getInfo(player: Player, pos: BlockPos): WailaInfo? {
        val novaState = WorldDataManager.getBlockState(pos)
        if (novaState != null) {
            val material = novaState.block
            
            return WAILA_INFO_PROVIDER.asSequence()
                .filterIsInstance<NovaWailaInfoProvider>()
                .lastOrNull { it.blocks == null || material in it.blocks }
                ?.getInfo(player, pos, novaState)
        } else {
            val block = pos.block
            val type = block.type
            
            return getCustomItemServiceInfo(player, block)
                ?: WAILA_INFO_PROVIDER.asSequence()
                    .filterIsInstance<VanillaWailaInfoProvider<BlockData>>()
                    .lastOrNull { it.materials == null || type in it.materials }
                    ?.getInfo(player, pos, block.blockData)
        }
    }
    
    @Suppress("DEPRECATION")
    private fun getCustomItemServiceInfo(player: Player, block: Block): WailaInfo? {
        val blockId = CustomItemServiceManager.getId(block)?.let { runCatching { Key.key(it) }.getOrNull() } ?: return null
        val blockName = CustomItemServiceManager.getName(block, player.locale) ?: return null
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(blockName, WailaLine.Alignment.CENTERED)
        lines += WailaLine(Component.text(blockId.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
        lines += ToolLine.getCustomItemServiceToolLine(player, block)
        
        return WailaInfo(blockId, lines)
    }
    
    private fun isBlacklisted(id: Key) =
        BLACKLISTED_BLOCKS.any { (namespaceRegex, nameRegex) ->
            namespaceRegex.matches(id.namespace()) && nameRegex.matches(id.value())
        }
    
}