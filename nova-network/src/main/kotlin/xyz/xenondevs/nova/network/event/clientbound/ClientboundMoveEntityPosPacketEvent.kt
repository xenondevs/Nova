package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundMoveEntityPosPacketEvent(
    player: Player,
    packet: ClientboundMoveEntityPacket.Pos
) : PlayerPacketEvent<ClientboundMoveEntityPacket.Pos>(player, packet) {
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var xa = packet.xa
        set(value) {
            field = value
            changed = true
        }
    
    var ya = packet.ya
        set(value) {
            field = value
            changed = true
        }
    
    var za = packet.za
        set(value) {
            field = value
            changed = true
        }
    
    var onGround = packet.isOnGround
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundMoveEntityPacket.Pos(entityId, xa, ya, za, onGround)
}
