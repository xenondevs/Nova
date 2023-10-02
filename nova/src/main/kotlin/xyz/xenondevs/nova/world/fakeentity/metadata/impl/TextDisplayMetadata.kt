package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.Display.TextDisplay.Align

class TextDisplayMetadata : DisplayMetadata() {
    
    var text: Component by entry(23, EntityDataSerializers.COMPONENT, Component.empty())
    var lineWidth: Int by entry(24, EntityDataSerializers.INT, 200)
    var backgroundColor: Int by entry(25, EntityDataSerializers.INT, 1073741824)
    var textOpacity: Int by entry(26, EntityDataSerializers.INT, -1)
    private val styleFlags = sharedFlags(27)
    var hasShadow: Boolean by styleFlags[0]
    var isSeeTrough: Boolean by styleFlags[1]
    var defaultBackground: Boolean by styleFlags[2]
    var alignment: Align
        get() = when {
            styleFlags.getState(3) -> Align.CENTER
            styleFlags.getState(4) -> Align.RIGHT
            styleFlags.getState(5) -> Align.LEFT
            else -> throw IllegalStateException()
        }
        set(value) {
            styleFlags.setState(3, value == Align.CENTER)
            styleFlags.setState(4, value == Align.RIGHT)
            styleFlags.setState(5, value == Align.LEFT)
        }
    
    init {
        // set center bit
        styleFlags.setState(3, true)
    }
    
}