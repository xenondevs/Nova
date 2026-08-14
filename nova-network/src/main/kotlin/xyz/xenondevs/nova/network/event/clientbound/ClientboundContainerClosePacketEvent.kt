package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundContainerClosePacketEvent(
    player: Player,
    packet: ClientboundContainerClosePacket
) : PlayerPacketEvent<ClientboundContainerClosePacket>(player, packet) {
    
    var containerId: Int = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundContainerClosePacket =
        ClientboundContainerClosePacket(containerId)
    
}
