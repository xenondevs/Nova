package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundMovePlayerPosRotPacketEvent(
    player: Player,
    packet: ServerboundMovePlayerPacket.PosRot
) : PlayerPacketEvent<ServerboundMovePlayerPacket.PosRot>(player, packet) {
    
    var x = packet.getX(0.0)
        set(value) {
            field = value
            changed = true
        }
    
    var y = packet.getY(0.0)
        set(value) {
            field = value
            changed = true
        }
    
    var z = packet.getZ(0.0)
        set(value) {
            field = value
            changed = true
        }
    
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
    
    override fun buildChangedPacket() = ServerboundMovePlayerPacket.PosRot(x, y, z, yRot, xRot, onGround, horizontalCollision)
}
