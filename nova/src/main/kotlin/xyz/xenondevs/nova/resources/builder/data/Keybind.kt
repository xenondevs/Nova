package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Keybind {
    
    @SerialName("key.jump")
    JUMP,
    
    @SerialName("key.sneak")
    SNEAK,
    
    @SerialName("key.sprint")
    SPRINT,
    
    @SerialName("key.left")
    LEFT,
    
    @SerialName("key.right")
    RIGHT,
    
    @SerialName("key.back")
    BACK,
    
    @SerialName("key.forward")
    FORWARD,
    
    @SerialName("key.attack")
    ATTACK,
    
    @SerialName("key.pickItem")
    PICK_ITEM,
    
    @SerialName("key.use")
    USE,
    
    @SerialName("key.drop")
    DROP,
    
    @SerialName("key.hotbar.1")
    HOTBAR_1,
    
    @SerialName("key.hotbar.2")
    HOTBAR_2,
    
    @SerialName("key.hotbar.3")
    HOTBAR_3,
    
    @SerialName("key.hotbar.4")
    HOTBAR_4,
    
    @SerialName("key.hotbar.5")
    HOTBAR_5,
    
    @SerialName("key.hotbar.6")
    HOTBAR_6,
    
    @SerialName("key.hotbar.7")
    HOTBAR_7,
    
    @SerialName("key.hotbar.8")
    HOTBAR_8,
    
    @SerialName("key.hotbar.9")
    HOTBAR_9,
    
    @SerialName("key.inventory")
    INVENTORY,
    
    @SerialName("key.swapOffhand")
    SWAP_OFFHAND,
    
    @SerialName("key.loadToolbarActivator")
    LOAD_TOOLBAR_ACTIVATOR,
    
    @SerialName("key.saveToolbarActivator")
    SAVE_TOOLBAR_ACTIVATOR,
    
    @SerialName("key.playerlist")
    PLAYER_LIST,
    
    @SerialName("key.chat")
    CHAT,
    
    @SerialName("key.command")
    COMMAND,
    
    @SerialName("key.socialInteractions")
    SOCIAL_INTERACTIONS,
    
    @SerialName("key.advancements")
    ADVANCEMENTS,
    
    @SerialName("key.spectatorOutlines")
    SPECTATOR_OUTLINES,
    
    @SerialName("key.screenshot")
    SCREENSHOT,
    
    @SerialName("key.smoothCamera")
    SMOOTH_CAMERA,
    
    @SerialName("key.fullscreen")
    FULLSCREEN,
    
    @SerialName("key.togglePerspective")
    TOGGLE_PERSPECTIVE
    
}