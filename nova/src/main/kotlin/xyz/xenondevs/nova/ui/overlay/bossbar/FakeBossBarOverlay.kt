package xyz.xenondevs.nova.ui.overlay.bossbar

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.world.BossEvent.BossBarColor
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.bossbar.BossBar
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.data.toPlainText
import xyz.xenondevs.nova.util.enumMapOf
import java.awt.Color
import kotlin.math.roundToInt
import net.minecraft.world.BossEvent.BossBarOverlay as BossBarStyle

private val INVISIBLE_COLORS by configReloadable {
    DEFAULT_CONFIG.getStringList("overlay.bossbar.invisible_colors")
        .mapTo(HashSet()) { BossBarColor.valueOf(it.uppercase()) }
}

private val COLOR_LOOKUP: Map<BossBarColor, ChatColor> = mapOf(
    BossBarColor.PINK to Color(255, 0, 199),
    BossBarColor.BLUE to Color(0, 198, 255),
    BossBarColor.RED to Color(255, 57, 0),
    BossBarColor.GREEN to Color(31, 255, 0),
    BossBarColor.YELLOW to Color(252, 255, 0),
    BossBarColor.PURPLE to Color(133, 0, 255),
    BossBarColor.WHITE to Color(255, 255, 255)
).mapValuesTo(enumMapOf()) { (_, color) -> ChatColor.of(color) }

private const val BOSS_BAR_LENGTH = 182
private const val HALF_BOSS_BAR_LENGTH = BOSS_BAR_LENGTH / 2
private const val STYLE_OVERLAY_LENGTH = 182

internal class FakeBossBarOverlay(
    private val player: Player,
    val bar: BossBar
) : BossBarOverlay() {
    
    override val width = 0
    override val endY = -12
    override val centerX = null
    
    private val barIdx: Int
        get() = BossBarOverlayManager.trackedBars[player]?.entries
            ?.indexOfFirst { (_, bar) -> this.bar == bar }
            ?: 0
    
    private val y: Int
        get() {
            var endY = BossBarOverlayManager.getEndY(player)
            if (endY != 0) {
                // make room if there is any other overlay above
                endY -= 12 // 5 for space, 7 for text
            }
            
            return endY - 19 * barIdx
        }
    
    override val barLevel: Int
        get() = y / 19 * -1
    
    override val components: Array<out BaseComponent>
        get() {
            val builder = ComponentBuilder()
            
            val barColor = bar.color
            val barRendered = barColor !in INVISIBLE_COLORS
            if (barRendered) {
                val color = COLOR_LOOKUP[bar.color]!!
                
                // boss bar image
                val font = "nova:bossbar/${y % 19}"
                
                builder
                    .append(MoveCharacters.getMovingComponent(-HALF_BOSS_BAR_LENGTH))
                    .append(TextComponent("\uF000")) // background
                    .color(color)
                    .font(font)
                    .append(MoveCharacters.getMovingComponent(-BOSS_BAR_LENGTH - 1))
                
                val progress = getProgressComponent(font, bar.progress)
                if (progress != null) {
                    val (progressComponent, progressWidth) = progress
                    builder
                        .append(progressComponent)
                        .color(color)
                        .append(MoveCharacters.getMovingComponent(-progressWidth - 1))
                }
                
                val style = getStyleComponent(font, bar.overlay)
                if (style != null) {
                    builder
                        .append(style)
                        .color(ChatColor.WHITE)
                        .append(MoveCharacters.getMovingComponent(-STYLE_OVERLAY_LENGTH - 1))
                }
            }
            
            // text
            val text = bar.name
            val textLength = DefaultFont.getStringLength(text.toPlainText(player.locale))
            val halfTextLength = textLength / 2
            
            builder
                .append(MoveCharacters.getMovingComponent((if (barRendered) HALF_BOSS_BAR_LENGTH else 0) - halfTextLength)) // move to the text start
                .color(ChatColor.WHITE)
                .append(DefaultFont.getVerticallyMovedText(text, y % 19)) // append text
                .append(MoveCharacters.getMovingComponent(-halfTextLength)) // move back to the center
            
            return builder.create()
        }
    
    private fun getProgressComponent(font: String, progress: Float): Pair<BaseComponent, Int>? {
        require(progress in 0f..1f)
        
        if (progress == 0f)
            return null
        
        val i = (progress * BOSS_BAR_LENGTH).roundToInt()
        val char = (0xFF00 + i).toChar().toString()
        val component = TextComponent(char).apply { this.font = font }
        return component to i
    }
    
    private fun getStyleComponent(font: String, style: BossBarStyle): BaseComponent? {
        val char = when (style) {
            BossBarStyle.PROGRESS -> null
            BossBarStyle.NOTCHED_6 -> "\uF001"
            BossBarStyle.NOTCHED_10 -> "\uF002"
            BossBarStyle.NOTCHED_12 -> "\uF003"
            BossBarStyle.NOTCHED_20 -> "\uF004"
        } ?: return null
        
        val component = TextComponent(char).apply { this.font = font }
        return component
    }
    
}