package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundPaddleBoatPacketEvent(
    player: Player,
    packet: ServerboundPaddleBoatPacket
) : PlayerPacketEvent<ServerboundPaddleBoatPacket>(player, packet) {
    
    var left = packet.left
        set(value) {
            field = value
            changed = true
        }
    
    var right = packet.right
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundPaddleBoatPacket(left, right)
}
