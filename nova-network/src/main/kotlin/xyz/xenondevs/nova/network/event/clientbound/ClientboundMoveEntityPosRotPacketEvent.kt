package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.VecDelta
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
    
    var delta: VecDelta = packet.positionDelta
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
        ClientboundMoveEntityPacket.PosRot(entityId, delta, Mth.packDegrees(yRot), Mth.packDegrees(xRot), onGround)
}
