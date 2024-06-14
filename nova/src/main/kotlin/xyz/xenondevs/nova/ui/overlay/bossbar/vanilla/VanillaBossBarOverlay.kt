package xyz.xenondevs.nova.ui.overlay.bossbar.vanilla

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.BossEvent.BossBarColor
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlay
import xyz.xenondevs.nova.util.bossbar.BossBar
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import java.awt.Color
import kotlin.math.roundToInt
import net.minecraft.world.BossEvent.BossBarOverlay as BossBarStyle

private val INVISIBLE_COLORS: Set<BossBarColor> by MAIN_CONFIG.entry<HashSet<BossBarColor>>("overlay", "bossbar", "invisible_colors")

private val COLOR_LOOKUP: Map<BossBarColor, TextColor> = mapOf(
    BossBarColor.PINK to Color(255, 0, 199),
    BossBarColor.BLUE to Color(0, 198, 255),
    BossBarColor.RED to Color(255, 57, 0),
    BossBarColor.GREEN to Color(31, 255, 0),
    BossBarColor.YELLOW to Color(252, 255, 0),
    BossBarColor.PURPLE to Color(133, 0, 255),
    BossBarColor.WHITE to Color(255, 255, 255)
).mapValuesTo(enumMap()) { (_, color) -> TextColor.color(color.rgb) }

private const val BOSS_BAR_FONT = "nova:bossbar"
private const val BOSS_BAR_LENGTH = 182
private const val HALF_BOSS_BAR_LENGTH = BOSS_BAR_LENGTH / 2
private const val STYLE_OVERLAY_LENGTH = 182

internal class VanillaBossBarOverlay(
    private val player: Player,
    val bar: BossBar
) : BossBarOverlay {
    
    override val offset = 0
    override val centerX = null
    override var component: Component = createComponent()
    
    override fun getWidth(locale: String): Float = 0f
    
    // this intentionally doesn't count the text itself into the vertical range, as that would result in an uneven spacing between different boss bars
    // https://i.imgur.com/MKxqciO.png
    override fun getVerticalRange(locale: String): IntRange = 2..6
    
    fun update() {
        component = createComponent()
    }
    
    private fun createComponent(): Component {
        val builder = Component.text()
        
        val barColor = bar.color
        val barRendered = barColor !in INVISIBLE_COLORS
        if (barRendered) {
            val color = COLOR_LOOKUP[bar.color]!!
            
            builder
                .move(-HALF_BOSS_BAR_LENGTH)
                .append(Component.text('\uF000').font(BOSS_BAR_FONT)) // background
                .move(-BOSS_BAR_LENGTH - 1)
            
            val progress = getProgressComponent(bar.progress, color)
            if (progress != null) {
                val (progressComponent, progressWidth) = progress
                builder
                    .append(progressComponent.color(color))
                    .move(-progressWidth - 1)
            }
            
            val style = getStyleComponent(bar.overlay)
            if (style != null) {
                builder
                    .append(style)
                    .move(-STYLE_OVERLAY_LENGTH - 1)
            }
        }
        
        // text
        val text = bar.name
        val textLength = CharSizes.calculateComponentWidth(text, player.locale)
        val halfTextLength = textLength / 2
        
        builder
            .move((if (barRendered) HALF_BOSS_BAR_LENGTH else 0) - halfTextLength) // move to the text start
            .append(text) // append text
            .move(-halfTextLength) // move back to the center
        
        return builder.build()
    }
    
    private fun getProgressComponent(progress: Float, color: TextColor): Pair<Component, Int>? {
        require(progress in 0f..1f)
        
        if (progress == 0f)
            return null
        
        val i = (progress * BOSS_BAR_LENGTH).roundToInt()
        val char = (0xFF00 + i).toChar().toString()
        val component = Component.text(char, Style.style(color)).font(BOSS_BAR_FONT)
        return component to i
    }
    
    private fun getStyleComponent(style: BossBarStyle): Component? {
        val char = when (style) {
            BossBarStyle.PROGRESS -> null
            BossBarStyle.NOTCHED_6 -> "\uF001"
            BossBarStyle.NOTCHED_10 -> "\uF002"
            BossBarStyle.NOTCHED_12 -> "\uF003"
            BossBarStyle.NOTCHED_20 -> "\uF004"
        } ?: return null
        
        return Component.text(char).font(BOSS_BAR_FONT)
    }
    
    override fun toString(): String {
        return "VanillaBossBarOverlay(text=${bar.name.toPlainText()})"
    }
    
}