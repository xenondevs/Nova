package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundContainerClosePacketEvent(
    player: Player,
    packet: ServerboundContainerClosePacket
) : PlayerPacketEvent<ServerboundContainerClosePacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundContainerClosePacket(containerId)
}
