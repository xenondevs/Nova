package xyz.xenondevs.nova.util.data

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention
import net.md_5.bungee.api.chat.HoverEvent
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.builder.content.font.FontChar
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters

class MovingComponentBuilder(private val locale: String) {
    
    private val builder = ComponentBuilder()
    val width: Int
        get() = CharSizes.calculateComponentWidth(create(), locale)
    
    constructor() : this("en_us")
    
    fun append(components: Array<out BaseComponent>, retention: FormatRetention = FormatRetention.ALL): MovingComponentBuilder {
        builder.append(components, retention)
        return this
    }
    
    fun append(component: BaseComponent, retention: FormatRetention = FormatRetention.ALL): MovingComponentBuilder {
        builder.append(component, retention)
        return this
    }
    
    fun append(text: String, retention: FormatRetention = FormatRetention.ALL): MovingComponentBuilder {
        builder.append(text, retention)
        return this
    }
    
    fun append(fontChar: FontChar): MovingComponentBuilder {
        return append(fontChar.component)
    }
    
    fun move(distance: Int): MovingComponentBuilder {
        builder.append(MoveCharacters.getMovingComponent(distance), FormatRetention.NONE)
        return this
    }
    
    fun moveToStart(): MovingComponentBuilder {
        return move(-width)
    }
    
    fun moveToCenter(): MovingComponentBuilder {
        return move(-width / 2)
    }
    
    fun moveTo(afterStart: Int): MovingComponentBuilder {
        return move(-width + afterStart)
    }
    
    fun color(color: ChatColor): MovingComponentBuilder {
        builder.color(color)
        return this
    }
    
    fun font(font: String): MovingComponentBuilder {
        builder.font(font)
        return this
    }
    
    fun bold(bold: Boolean): MovingComponentBuilder {
        builder.bold(bold)
        return this
    }
    
    fun italic(italic: Boolean): MovingComponentBuilder {
        builder.italic(italic)
        return this
    }
    
    fun underlined(underlined: Boolean): MovingComponentBuilder {
        builder.underlined(underlined)
        return this
    }
    
    fun strikethrough(strikethrough: Boolean): MovingComponentBuilder {
        builder.strikethrough(strikethrough)
        return this
    }
    
    fun obfuscated(obfuscated: Boolean): MovingComponentBuilder {
        builder.obfuscated(obfuscated)
        return this
    }
    
    fun insertion(insertion: String): MovingComponentBuilder {
        builder.insertion(insertion)
        return this
    }
    
    fun event(clickEvent: ClickEvent): MovingComponentBuilder {
        builder.event(clickEvent)
        return this
    }
    
    fun event(hoverEvent: HoverEvent): MovingComponentBuilder {
        builder.event(hoverEvent)
        return this
    }
    
    fun reset(): MovingComponentBuilder {
        builder.retain(FormatRetention.NONE)
        return this
    }
    
    fun retain(retention: FormatRetention): MovingComponentBuilder {
        builder.retain(retention)
        return this
    }
    
    fun create(): Array<BaseComponent> {
        return builder.create()
    }
    
}