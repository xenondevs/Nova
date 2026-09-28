package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundMovePlayerRotPacketEvent(
    player: Player,
    packet: ServerboundMovePlayerPacket.Rot
) : PlayerPacketEvent<ServerboundMovePlayerPacket.Rot>(player, packet) {
    
    var yRot = packet.getYRot(0f)
        set(value) {
            field = value
            changed = true
        }
    
    var xRot = packet.getXRot(0f)
        set(value) {
            field = value
            changed = true
        }
    
    var onGround = packet.isOnGround
        set(value) {
            field = value
            changed = true
        }
    
    var horizontalCollision = packet.horizontalCollision()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, horizontalCollision)
}
