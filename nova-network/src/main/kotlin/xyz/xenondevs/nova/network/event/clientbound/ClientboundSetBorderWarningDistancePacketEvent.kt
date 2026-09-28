package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetBorderWarningDistancePacket

class ClientboundSetBorderWarningDistancePacketEvent(
    player: Player,
    packet: ClientboundSetBorderWarningDistancePacket
) : PlayerPacketEvent<ClientboundSetBorderWarningDistancePacket>(player, packet) {
    
    var warningBlocks = packet.warningBlocks
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetBorderWarningDistancePacket =
        ClientboundSetBorderWarningDistancePacket(warningBlocks)
}
