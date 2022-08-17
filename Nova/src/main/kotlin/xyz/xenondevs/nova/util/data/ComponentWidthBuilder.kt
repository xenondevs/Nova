package xyz.xenondevs.nova.util.data

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont

class ComponentWidthBuilder(private val locale: String) {
    
    private val builder = ComponentBuilder()
    private var width = 0
    
    fun append(components: Array<BaseComponent>): ComponentWidthBuilder {
        builder.append(components)
        width += DefaultFont.getStringLength(components.toPlainText(locale))
        return this
    }
    
    fun append(components: Array<BaseComponent>, width: Int): ComponentWidthBuilder {
        builder.append(components)
        this.width += width
        return this
    }
    
    fun append(component: BaseComponent): ComponentWidthBuilder {
        builder.append(component)
        width += DefaultFont.getStringLength(component.toPlainText(locale))
        return this
    }
    
    fun append(component: BaseComponent, width: Int): ComponentWidthBuilder {
        builder.append(component)
        this.width += width
        return this
    }
    
    fun append(text: String): ComponentWidthBuilder {
        builder.append(text)
        width += DefaultFont.getStringLength(text)
        return this
    }
    
    fun color(color: ChatColor): ComponentWidthBuilder {
        builder.color(color)
        return this
    }
    
    fun font(font: String): ComponentWidthBuilder {
        builder.font(font)
        return this
    }
    
    fun create(): Pair<Array<BaseComponent>, Int> {
        return builder.create() to width
    }
    
}