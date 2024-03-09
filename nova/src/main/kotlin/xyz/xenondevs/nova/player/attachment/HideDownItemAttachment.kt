package xyz.xenondevs.nova.player.attachment

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * A special type of [ItemAttachment] that gets hidden for the player of they look down
 */
class HideDownItemAttachment(
    private val pitchThreshold: Float,
    player: Player,
    itemStack: ItemStack,
    translation: Vector3fc = Vector3f(0f, 0f, 0f),
    scale: Vector3fc = Vector3f(1f, 1f, 1f),
) : ItemAttachment(player, itemStack, translation, scale) {
    
    private var hidden = false
    
    override fun handleTick() {
        super.handleTick()
        
        val pitch = player.location.pitch
        if (pitch >= pitchThreshold && !hidden) {
            // hide display entity for attachment carrier
            passenger.despawn(player)
            hidden = true
        } else if (hidden && pitch < pitchThreshold) {
            // show display entity again
            passenger.spawn(player)
            hidden = false
        }
    }
    
}