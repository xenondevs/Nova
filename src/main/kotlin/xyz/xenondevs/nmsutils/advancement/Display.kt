package xyz.xenondevs.nmsutils.advancement

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.advancements.DisplayInfo
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.internal.util.nmsStack
import xyz.xenondevs.nmsutils.internal.util.resourceLocation
import xyz.xenondevs.nmsutils.internal.util.toComponent

enum class FrameType {
    TASK,
    CHALLENGE,
    GOAL
}

class Display(
    val frame: FrameType,
    val background: String?,
    val icon: ItemStack,
    val title: Array<BaseComponent>,
    val description: Array<BaseComponent>,
    val showToast: Boolean,
    val announceToChat: Boolean,
    val hidden: Boolean
) {
    
    companion object : Adapter<Display, DisplayInfo> {
        
        override fun toNMS(value: Display): DisplayInfo =
            DisplayInfo(
                value.icon.nmsStack,
                value.title.toComponent(), value.description.toComponent(),
                value.background?.resourceLocation,
                net.minecraft.advancements.FrameType.values()[value.frame.ordinal],
                value.showToast,
                value.announceToChat,
                value.hidden
            )
        
    }
    
    @AdvancementDsl
    class Builder {
        
        private var frame: FrameType = FrameType.TASK
        private var background: String? = null
        
        private var icon: ItemStack? = null
        
        private var title: Array<BaseComponent>? = null
        private var description: Array<BaseComponent>? = null
        
        private var showToast = true
        private var announceToChat = true
        private var hidden = false
        
        fun frame(frame: FrameType) {
            this.frame = frame
        }
        
        fun background(background: String) {
            this.background = background
        }
        
        fun icon(icon: ItemStack) {
            this.icon = icon
        }
        
        fun title(title: Array<BaseComponent>) {
            this.title = title
        }
        
        fun title(title: BaseComponent) {
            this.title = arrayOf(title)
        }
        
        fun title(title: String) {
            this.title = TextComponent.fromLegacyText(title)
        }
        
        fun description(description: Array<BaseComponent>) {
            this.description = description
        }
        
        fun description(description: BaseComponent) {
            this.description = arrayOf(description)
        }
        
        fun description(description: String) {
            this.description = TextComponent.fromLegacyText(description)
        }
        
        fun showToast(showToast: Boolean) {
            this.showToast = showToast
        }
        
        fun announceToChat(announceToChat: Boolean) {
            this.announceToChat = announceToChat
        }
        
        fun hidden(hidden: Boolean) {
            this.hidden = hidden
        }
        
        internal fun build(): Display {
            checkNotNull(icon) { "Display icon is not set" }
            checkNotNull(title) { "Display title is not set" }
            checkNotNull(description) { "Display description is not set" }
            
            return Display(frame, background, icon!!, title!!, description!!, showToast, announceToChat, hidden)
            
        }
        
    }
    
}