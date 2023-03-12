package xyz.xenondevs.nmsutils.advancement

import net.kyori.adventure.text.Component
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.advancements.DisplayInfo
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.adapter.Adapter
import xyz.xenondevs.nmsutils.internal.util.nmsStack
import xyz.xenondevs.nmsutils.internal.util.resourceLocation
import xyz.xenondevs.nmsutils.internal.util.toJson
import xyz.xenondevs.nmsutils.internal.util.toNmsComponent

enum class FrameType {
    TASK,
    CHALLENGE,
    GOAL
}

class Display private constructor(
    val frame: FrameType,
    val background: String?,
    val icon: ItemStack,
    val titleJson: String,
    val descriptionJson: String,
    val showToast: Boolean,
    val announceToChat: Boolean,
    val hidden: Boolean
) {
    
    constructor(
        frame: FrameType,
        background: String?,
        icon: ItemStack,
        title: Array<out BaseComponent>,
        description: Array<out BaseComponent>,
        showToast: Boolean,
        announceToChat: Boolean,
        hidden: Boolean
    ) : this(
        frame,
        background,
        icon,
        title.toJson(),
        description.toJson(),
        showToast,
        announceToChat,
        hidden
    )
    
    constructor(
        frame: FrameType,
        background: String?,
        icon: ItemStack,
        title: Component,
        description: Component,
        showToast: Boolean,
        announceToChat: Boolean,
        hidden: Boolean
    ) : this (
        frame,
        background,
        icon,
        title.toJson(),
        description.toJson(),
        showToast,
        announceToChat,
        hidden
    )
    
    companion object : Adapter<Display, DisplayInfo> {
        
        override fun toNMS(value: Display): DisplayInfo =
            DisplayInfo(
                value.icon.nmsStack,
                value.titleJson.toNmsComponent(), value.descriptionJson.toNmsComponent(),
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
        
        private var titleJson: String? = null
        private var descriptionJson: String? = null
        
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
        
        fun title(title: Array<out BaseComponent>) {
            this.titleJson = title.toJson()
        }
        
        fun title(title: BaseComponent) {
            this.titleJson = arrayOf(title).toJson()
        }
        
        fun title(title: Component) {
            this.titleJson = title.toJson()
        }
        
        fun title(title: String) {
            this.titleJson = TextComponent.fromLegacyText(title).toJson()
        }
        
        fun description(description: Array<out BaseComponent>) {
            this.descriptionJson = description.toJson()
        }
        
        fun description(description: BaseComponent) {
            this.descriptionJson = arrayOf(description).toJson()
        }
        
        fun description(description: Component) {
            this.descriptionJson = description.toJson()
        }
        
        fun description(description: String) {
            this.descriptionJson = TextComponent.fromLegacyText(description).toJson()
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
            val icon = icon
            val titleJson = titleJson
            val descriptionJson = descriptionJson
            
            checkNotNull(icon) { "Display icon is not set" }
            checkNotNull(titleJson) { "Display title is not set" }
            checkNotNull(descriptionJson) { "Display description is not set" }
            
            return Display(frame, background, icon, titleJson, descriptionJson, showToast, announceToChat, hidden)
            
        }
        
    }
    
}