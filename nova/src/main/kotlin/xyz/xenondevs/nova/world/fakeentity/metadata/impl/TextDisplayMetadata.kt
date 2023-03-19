package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.Display.TextDisplay.Align

class TextDisplayMetadata : DisplayMetadata() {
    
    private val sharedFlags = sharedFlags(26)
    
    var text: Component by entry(22, EntityDataSerializers.COMPONENT, Component.empty())
    var lineWidth: Int by entry(23, EntityDataSerializers.INT, 200)
    var backgroundColor: Int by entry(24, EntityDataSerializers.INT, 1073741824)
    var textOpacity: Int by entry(25, EntityDataSerializers.INT, -1)
    var hasShadow: Boolean by sharedFlags[0]
    var isSeeTrough: Boolean by sharedFlags[1]
    var defaultBackground: Boolean by sharedFlags[2]
    var alignment: Align
        get() = when {
            sharedFlags.getState(3) -> Align.CENTER
            sharedFlags.getState(4) -> Align.RIGHT
            sharedFlags.getState(5) -> Align.LEFT
            else -> throw IllegalStateException()
        }
        set(value) {
            sharedFlags.setState(3, value == Align.CENTER)
            sharedFlags.setState(4, value == Align.RIGHT)
            sharedFlags.setState(5, value == Align.LEFT)
        }
    
    init {
        // set center bit
        sharedFlags.setState(3, true)
    }
    
}