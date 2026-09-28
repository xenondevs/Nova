package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.util.Mth
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundMoveEntityRotPacketEvent(
    player: Player,
    packet: ClientboundMoveEntityPacket.Rot
) : PlayerPacketEvent<ClientboundMoveEntityPacket.Rot>(player, packet) {
    
    var entityId = packet.entityId
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
        ClientboundMoveEntityPacket.Rot(entityId, Mth.packDegrees(yRot), Mth.packDegrees(xRot), onGround)
}
