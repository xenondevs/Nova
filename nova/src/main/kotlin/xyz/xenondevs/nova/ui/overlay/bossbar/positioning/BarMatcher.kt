package xyz.xenondevs.nova.ui.overlay.bossbar.positioning

import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.serialization.kotlinx.BarMatcherSerializerText
import xyz.xenondevs.nova.serialization.kotlinx.BarOriginSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.RegexSerializer
import xyz.xenondevs.nova.serialization.kotlinx.UUIDAsStringSerializer
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import java.util.*
import org.bukkit.plugin.Plugin as BukkitPlugin

data class BarMatchInfo(
    val uuid: UUID?,
    val bossBar: BossBar?,
    val barIndex: Int?,
    val origin: BarOrigin?,
    val overlay: Key? = null
) {
    
    companion object {
        
        fun fromAddon(id: Key): BarMatchInfo =
            BarMatchInfo(null, null, null, null, id)
        
    }
    
}

/**
 * Matches boss bars based on [BarMatchInfo].
 */
@Serializable
sealed interface BarMatcher {
    
    /**
     * Tests if the given [info] matches this matcher.
     */
    fun test(info: BarMatchInfo): Boolean
    
    /**
     * Matches bars with [uuid].
     */
    @SerialName("uuid")
    @Serializable
    class Id(
        @Serializable(with = UUIDAsStringSerializer::class)
        private val uuid: UUID
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.uuid == uuid
    }
    
    /**
     * Matches bars at [index].
     */
    @SerialName("index")
    @Serializable
    class Index(
        private val index: Int
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.barIndex == index
    }
    
    /**
     * Matches bars with a name that matches [regex].
     */
    @SerialName("text")
    @KeepGeneratedSerializer
    @Serializable(with = BarMatcherSerializerText::class)
    class Text(
        @Serializable(with = RegexSerializer::class)
        private val regex: Regex
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.bossBar?.name()?.toPlainText()?.matches(regex) ?: false
    }
    
    /**
     * Matches bars of [origin].
     */
    @SerialName("origin")
    @Serializable
    class Origin(
        private val origin: BarOrigin
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.origin == origin
    }
    
    /**
     * Matches bars that are part of a bossbar overlay with id [overlay].
     */
    @SerialName("overlay")
    @Serializable
    class Overlay(
        @Serializable(with = KeySerializer::class)
        private val overlay: Key
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = info.overlay == overlay
    }
    
    /**
     * Matches if any of the [matchers] match.
     */
    @SerialName("or")
    @Serializable
    class CombinedAny(
        private val matchers: List<BarMatcher>
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = matchers.any { it.test(info) }
    }
    
    /**
     * Matches if all [matchers] match.
     */
    @SerialName("and")
    @Serializable
    class CombinedAll(
        private val matchers: List<BarMatcher>
    ) : BarMatcher {
        override fun test(info: BarMatchInfo) = matchers.all { it.test(info) }
    }
    
    /**
     * Matches all bars.
     */
    data object True : BarMatcher {
        override fun test(info: BarMatchInfo) = true
    }
    
    /**
     * Matches no bars.
     */
    data object False : BarMatcher {
        override fun test(info: BarMatchInfo) = false
    }
    
}

/**
 * The origin of a boss bar.
 */
@Serializable(with = BarOriginSerializer::class)
sealed interface BarOrigin {
    
    /**
     * The boss bar comes from Minecraft itself, e.g., the Ender Dragon or via `/bossbar`.
     */
    @Serializable(with = BarOriginSerializer.Minecraft::class)
    data object Minecraft : BarOrigin
    
    /**
     * The boss bar comes from [plugin].
     */
    @Serializable(with = BarOriginSerializer.Plugin::class)
    data class Plugin(
        /**
         * The plugin that created the boss bar.
         */
        val plugin: BukkitPlugin
    ) : BarOrigin
    
}