package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundMovePlayerStatusOnlyPacketEvent(
    player: Player,
    packet: ServerboundMovePlayerPacket.StatusOnly
) : PlayerPacketEvent<ServerboundMovePlayerPacket.StatusOnly>(player, packet) {
    
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
    
    override fun buildChangedPacket() = ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision)
}
