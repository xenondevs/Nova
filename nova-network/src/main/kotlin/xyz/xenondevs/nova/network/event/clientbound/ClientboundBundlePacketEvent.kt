package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundBundlePacketEvent(
    player: Player,
    packet: ClientboundBundlePacket
) : PlayerPacketEvent<ClientboundBundlePacket>(player, packet) {
    
    var packets: Iterable<Packet<in ClientGamePacketListener>> = packet.subPackets()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBundlePacket =
        ClientboundBundlePacket(packets)
    
}
