package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.VecDelta
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
    
    var delta: VecDelta = packet.positionDelta
        set(value) {
            field = value
            changed = true
        }
    
    var onGround = packet.isOnGround
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundMoveEntityPacket.Pos(entityId, delta, onGround)
}
