package xyz.xenondevs.nmsutils.advancement

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.minecraft.advancements.DisplayInfo
import net.minecraft.advancements.FrameType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.nmsutils.internal.util.nmsStack
import xyz.xenondevs.nmsutils.internal.util.toNmsComponent
import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.inventory.ItemStack as BukkitItemStack

@AdvancementDsl
class DisplayInfoBuilder {
    
    private var frame: FrameType = FrameType.TASK
    private var background: ResourceLocation? = null
    private var icon = ItemStack.EMPTY
    private var titleJson: Component = Component.empty()
    private var descriptionJson: Component = Component.empty()
    private var showToast = true
    private var announceToChat = true
    private var hidden = false
    
    fun frame(frame: FrameType) {
        this.frame = frame
    }
    
    fun background(background: ResourceLocation) {
        this.background = background
    }
    
    fun background(background: String) {
        this.background = ResourceLocation(background)
    }
    
    fun icon(icon: ItemStack) {
        this.icon = icon
    }
    
    fun icon(icon: BukkitItemStack) {
        this.icon = icon.nmsStack
    }
    
    fun title(title: Array<out BaseComponent>) {
        this.titleJson = title.toNmsComponent()
    }
    
    fun title(title: BaseComponent) {
        this.titleJson = arrayOf(title).toNmsComponent()
    }
    
    fun title(title: AdventureComponent) {
        this.titleJson = title.toNmsComponent()
    }
    
    fun title(title: String) {
        this.titleJson = TextComponent.fromLegacyText(title).toNmsComponent()
    }
    
    fun description(description: Array<out BaseComponent>) {
        this.descriptionJson = description.toNmsComponent()
    }
    
    fun description(description: BaseComponent) {
        this.descriptionJson = arrayOf(description).toNmsComponent()
    }
    
    fun description(description: AdventureComponent) {
        this.descriptionJson = description.toNmsComponent()
    }
    
    fun description(description: String) {
        this.descriptionJson = TextComponent.fromLegacyText(description).toNmsComponent()
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
    
    internal fun build(): DisplayInfo =
        DisplayInfo(icon, titleJson, descriptionJson, background, frame, showToast, announceToChat, hidden)
    
}