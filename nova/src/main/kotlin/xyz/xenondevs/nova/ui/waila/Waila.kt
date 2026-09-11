package xyz.xenondevs.nova.ui.waila

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapEach
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries.WAILA_INFO_PROVIDER
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.getCustomItemServiceToolText
import xyz.xenondevs.nova.ui.waila.overlay.WailaOverlayCompound
import xyz.xenondevs.nova.util.capitalizeAll
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.data.WildcardUtils
import xyz.xenondevs.nova.util.serverTick
import xyz.xenondevs.nova.world.block.blockType
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

private val WAILA_INFO_PROVIDERS: Map<BlockType, WailaInfoProvider<BlockData>>
    by WAILA_INFO_PROVIDER.entrySet.flatMap { infoProviders: Set<WailaInfoProvider<*>> ->
        combinedProvider(
            infoProviders.map { wip -> wip.blocks.map { entries -> entries to wip } }
        ) { list: List<Pair<Set<BlockType>, WailaInfoProvider<*>>> ->
            buildMap {
                for ([blocks, infoProvider] in list) for (block in blocks) {
                    val current = this[block]
                    if (current == null || infoProvider.priority > current.priority) {
                        @Suppress("UNCHECKED_CAST")
                        put(block, infoProvider as WailaInfoProvider<BlockData>)
                    }
                }
            }
        }
    }

internal class Waila(
    val player: Player,
    backgroundEnabled: Boolean
) {
    
    private var lastPosUpdate: Int = 0
    private var lastDataUpdate: Int = 0
    private var lookingAt: Block? = null
    
    private var active = false
    private val overlay = WailaOverlayCompound(player)
    
    private var prevInfo: WailaInfo? = null
    
    var backgroundEnabled = backgroundEnabled
        set(value) {
            if (field == value)
                return
            field = value
            prevInfo?.let { overlay.update(it.icon, it.lines, value) }
        }
    
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
            val pos = player.getTargetBlockExact(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)!!.value.roundToInt())
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
    
    private fun update(block: Block?) {
        lookingAt = block
        setActive(tryUpdate(block))
    }
    
    private fun tryUpdate(block: Block?): Boolean {
        if (block != null) {
            if (isBlacklisted(block.blockType.key))
                return false
            
            val info = getInfo(player, block)
                ?: return false
            
            if (info != prevInfo) {
                prevInfo = info
                overlay.update(info.icon, info.lines, backgroundEnabled)
            }
            
            return true
        }
        
        return false
    }
    
    private fun getInfo(player: Player, block: Block): WailaInfo? {
        val type = block.blockType
        
        return getCustomItemServiceInfo(player, block)
            ?: WAILA_INFO_PROVIDERS[type]?.getInfo(player, block, block.blockData)
    }
    
    private fun getCustomItemServiceInfo(player: Player, block: Block): WailaInfo? {
        val blockId = CustomItemServiceManager.getId(block)?.let { runCatching { Key.key(it) }.getOrNull() } ?: return null
        val blockName = CustomItemServiceManager.getName(block, @Suppress("DEPRECATION") player.locale) ?: return null
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(
            Component.text()
                .append(blockName)
                .append(Component.text(" "))
                .append(getCustomItemServiceToolText(player, block))
                .build(),
            WailaLine.Alignment.LEFT
        )
        lines += WailaLine(
            Component.text()
                .move(1) // to adjust for italic
                .append(Component.text(
                    blockId.namespace()
                        .replace('_', ' ')
                        .replace('-', ' ')
                        .capitalizeAll(),
                    NamedTextColor.BLUE,
                    TextDecoration.ITALIC
                ))
                .shadowColor(ShadowColor.none())
                .build(),
            WailaLine.Alignment.LEFT
        )
        
        return WailaInfo(blockId, lines)
    }
    
    private fun isBlacklisted(id: Key) =
        BLACKLISTED_BLOCKS.any { [namespaceRegex, nameRegex] ->
            namespaceRegex.matches(id.namespace()) && nameRegex.matches(id.value())
        }
    
}
