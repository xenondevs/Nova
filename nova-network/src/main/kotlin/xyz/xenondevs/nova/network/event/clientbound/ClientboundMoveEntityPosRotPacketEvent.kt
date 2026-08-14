package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.util.Mth
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundMoveEntityPosRotPacketEvent(
    player: Player,
    packet: ClientboundMoveEntityPacket.PosRot
) : PlayerPacketEvent<ClientboundMoveEntityPacket.PosRot>(player, packet) {
    
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
    
    var yRot = packet.yRot
        set(value) {
            field = value
            changed = true
        }
    
    var xRot = packet.xRot
        set(value) {
            field = value
            changed = true
        }
    
    var onGround = packet.isOnGround
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() =
        ClientboundMoveEntityPacket.PosRot(entityId, xa, ya, za, Mth.packDegrees(yRot), Mth.packDegrees(xRot), onGround)
}
