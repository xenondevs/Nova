package xyz.xenondevs.nova.util.data

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.builder.content.FontChar
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters

class MovingComponentBuilder(private val locale: String) {
    
    private val builder = ComponentBuilder()
    val width: Int
        get() = CharSizes.calculateComponentLength(create(), locale)
    
    constructor() : this("en_us")
    
    fun append(components: Array<BaseComponent>): MovingComponentBuilder {
        builder.append(components)
        return this
    }
    
    fun append(component: BaseComponent): MovingComponentBuilder {
        builder.append(component)
        return this
    }
    
    fun append(text: String): MovingComponentBuilder {
        builder.append(text)
        return this
    }
    
    fun append(fontChar: FontChar): MovingComponentBuilder {
        return append(fontChar.component)
    }
    
    fun move(distance: Int): MovingComponentBuilder {
        builder.append(MoveCharacters.getMovingComponent(distance))
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
    
    fun create(): Array<BaseComponent> {
        return builder.create()
    }
    
}