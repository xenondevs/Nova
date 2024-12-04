package xyz.xenondevs.nova.ui.overlay.bossbar.positioning

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.util.bossbar.BossBar
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import java.util.*
import org.bukkit.plugin.Plugin as BukkitPlugin

data class BarMatchInfo(
    val bossBar: BossBar?,
    val barIndex: Int?,
    val origin: BarOrigin?
) {
    
    companion object {
        
        fun fromBossBar(bossBar: BossBar, barIndex: Int): BarMatchInfo =
            BarMatchInfo(bossBar, barIndex, BarOrigin.Minecraft)
        
        fun fromPlugin(bossBar: BossBar, barIndex: Int, plugin: BukkitPlugin): BarMatchInfo =
            BarMatchInfo(bossBar, barIndex, BarOrigin.Plugin(plugin))
        
        fun fromAddon(id: Key): BarMatchInfo =
            BarMatchInfo(null, null, BarOrigin.Addon(id))
        
    }
    
}

interface BarMatcher {
    
    fun test(info: BarMatchInfo): Boolean
    
    class Id(private val uuid: UUID) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.bossBar?.id == uuid
    }
    
    class Index(private val index: Int) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.barIndex == index
    }
    
    class Text(private val regex: Regex) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.bossBar?.name?.toPlainText()?.matches(regex) ?: false
    }
    
    class Origin(private val origin: BarOrigin) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.origin == origin
    }
    
    class CombinedAny(private val matchers: List<BarMatcher>) : BarMatcher {
        override fun test(info: BarMatchInfo) = matchers.any { it.test(info) }
    }
    
    class CombinedAll(private val matchers: List<BarMatcher>) : BarMatcher {
        override fun test(info: BarMatchInfo) = matchers.all { it.test(info) }
    }
    
    companion object {
        
        val TRUE = object : BarMatcher {
            override fun test(info: BarMatchInfo) = true
        }
        
        val FALSE = object : BarMatcher {
            override fun test(info: BarMatchInfo) = false
        }
        
    }
    
}

sealed interface BarOrigin {
    
    data object Minecraft : BarOrigin
    
    data class Plugin(val plugin: BukkitPlugin) : BarOrigin
    
    data class Addon(val id: Key) : BarOrigin
    
}