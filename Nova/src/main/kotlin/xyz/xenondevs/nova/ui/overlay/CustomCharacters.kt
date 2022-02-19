package xyz.xenondevs.nova.ui.overlay

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.data.getResourceAsStream
import kotlin.math.abs

object CoreGUITexture {
    
    val EMPTY_GUI = GUITexture.of("nova:empty")
    val SEARCH = GUITexture.of("nova:search")
    val ITEMS_0 = GUITexture.of("nova:items_0")
    val ITEMS_1 = GUITexture.of("nova:items_1")
    val ITEMS_2 = GUITexture.of("nova:items_2")
    val ITEMS_3 = GUITexture.of("nova:items_3")
    val ITEMS_4 = GUITexture.of("nova:items_4")
    val RECIPE_CRAFTING = GUITexture.of("nova:recipe_crafting")
    val RECIPE_SMELTING = GUITexture.of("nova:recipe_smelting")
    val RECIPE_SMITHING = GUITexture.of("nova:recipe_smithing")
    val RECIPE_CONVERSION = GUITexture.of("nova:recipe_conversion")
    
}

class GUITexture(private val data: GUIData) {
    
    val component: BaseComponent = ComponentBuilder(data.char.toString())
        .font("nova:gui")
        .color(ChatColor.WHITE)
        .create()[0]
    
    fun getTitle(translate: String): Array<BaseComponent> {
        return getTitle(TranslatableComponent(translate))
    }
    
    fun getTitle(title: TranslatableComponent): Array<BaseComponent> {
        return ComponentBuilder()
            .append(MoveCharacters.getMovingComponent(-8)) // move to side to place overlay
            .append(data.char.toString())
            .font("nova:gui")
            .color(ChatColor.WHITE)
            .append(MoveCharacters.getMovingComponent(-data.width + 7)) // move back to start
            .append(title)
            .font("default")
            .color(ChatColor.DARK_GRAY)
            .create()
        
    }
    
    companion object {
        
        internal fun of(id: String) = GUITexture(Resources.getGUIData(id))
        
        fun of(addon: Addon, name: String) = of(name.addNamespace(addon.description.id))
        
    }
    
}

object MoveCharacters {
    
    private val componentCache = HashMap<Int, BaseComponent>()
    private val charWidths = getResourceAsStream("char_widths.bin")!!.readAllBytes()
    
    private fun getMovingString(distance: Int): String {
        val start = if (distance < 0) '\uF000'.code else '\uF100'.code
        val num = abs(distance)
        val buffer = StringBuffer()
        for (bit in 0 until 31)
            if (num and (1 shl bit) != 0)
                buffer.append((start + bit).toChar())
        
        return buffer.toString()
    }
    
    fun getMovingComponent(distance: Int): BaseComponent {
        return componentCache.getOrPut(distance) {
            ComponentBuilder(getMovingString(distance))
                .font("nova:move")
                .create()[0]
        }
    }
    
    fun getStringLength(string: String): Int {
        var size = 0
        string.toCharArray().forEach { size += getCharWidth(it) }
        return size
    }
    
    private fun getCharWidth(char: Char): Int =
        charWidths[char.code].toInt()
    
}
