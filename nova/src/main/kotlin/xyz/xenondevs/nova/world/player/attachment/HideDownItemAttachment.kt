package xyz.xenondevs.nova.world.player.attachment

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
    
    override fun handleTick() {
        super.handleTick()
        
        val pitch = player.location.pitch
        if (pitch >= pitchThreshold && passenger.viewerBlacklist.isEmpty()) {
            // hide display entity for attachment carrier
            passenger.viewerBlacklist = setOf(player.uniqueId)
        } else if (passenger.viewerBlacklist.isNotEmpty() && pitch < pitchThreshold) {
            // show display entity again
            passenger.viewerBlacklist = emptySet()
        }
    }
    
}