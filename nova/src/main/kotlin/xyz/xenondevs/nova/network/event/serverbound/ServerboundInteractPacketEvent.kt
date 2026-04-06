package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundInteractPacketEvent(
    player: Player,
    packet: ServerboundInteractPacket
) : PlayerPacketEvent<ServerboundInteractPacket>(player, packet) {
    
    var entityId: Int = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var hand: InteractionHand = packet.hand
        set(value) {
            field = value
            changed = true
        }
    
    var location: Vec3 = packet.location
        set(value) {
            field = value
            changed = true
        }
    
    var isUsingSecondaryAction: Boolean = packet.usingSecondaryAction()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundInteractPacket =
        ServerboundInteractPacket(entityId, hand, location, isUsingSecondaryAction)
    
}