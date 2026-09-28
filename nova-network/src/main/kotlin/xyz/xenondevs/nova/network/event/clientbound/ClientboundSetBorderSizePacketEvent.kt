package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetBorderSizePacket

class ClientboundSetBorderSizePacketEvent(
    player: Player,
    packet: ClientboundSetBorderSizePacket
) : PlayerPacketEvent<ClientboundSetBorderSizePacket>(player, packet) {
    
    var size = packet.size
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetBorderSizePacket =
        ClientboundSetBorderSizePacket(size)
}
