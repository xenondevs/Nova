package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.ServerboundPlaceRecipePacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundPlaceRecipePacketEvent(
    player: Player,
    packet: ServerboundPlaceRecipePacket
) : PlayerPacketEvent<ServerboundPlaceRecipePacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    var recipe = packet.recipe
        set(value) {
            field = value
            changed = true
        }
    var shiftDown = packet.isShiftDown
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPlaceRecipePacket {
        return ServerboundPlaceRecipePacket(containerId, recipe, shiftDown)
    }
    
}