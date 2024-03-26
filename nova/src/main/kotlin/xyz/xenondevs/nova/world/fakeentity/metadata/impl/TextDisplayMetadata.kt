package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.kyori.adventure.text.Component
import net.minecraft.network.syncher.EntityDataSerializers
import org.bukkit.entity.TextDisplay.TextAlignment
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent

class TextDisplayMetadata : DisplayMetadata() {
    
    var text: Component by entry(23, EntityDataSerializers.COMPONENT, Component.empty()) { it.toNMSComponent() }
    var lineWidth: Int by entry(24, EntityDataSerializers.INT, 200)
    var backgroundColor: Int by entry(25, EntityDataSerializers.INT, 1073741824)
    var textOpacity: Int by entry(26, EntityDataSerializers.INT, -1)
    private val styleFlags = sharedFlags(27)
    var hasShadow: Boolean by styleFlags[0]
    var isSeeTrough: Boolean by styleFlags[1]
    var defaultBackground: Boolean by styleFlags[2]
    var alignment: TextAlignment
        get() = when {
            styleFlags.getState(3) -> TextAlignment.CENTER
            styleFlags.getState(4) -> TextAlignment.RIGHT
            styleFlags.getState(5) -> TextAlignment.LEFT
            else -> throw IllegalStateException()
        }
        set(value) {
            styleFlags.setState(3, value == TextAlignment.CENTER)
            styleFlags.setState(4, value == TextAlignment.RIGHT)
            styleFlags.setState(5, value == TextAlignment.LEFT)
        }
    
    init {
        // set center bit
        styleFlags.setState(3, true)
    }
    
}