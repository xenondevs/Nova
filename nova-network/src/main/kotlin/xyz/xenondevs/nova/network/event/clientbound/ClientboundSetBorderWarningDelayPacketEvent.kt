package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetBorderWarningDelayPacket

class ClientboundSetBorderWarningDelayPacketEvent(
    player: Player,
    packet: ClientboundSetBorderWarningDelayPacket
) : PlayerPacketEvent<ClientboundSetBorderWarningDelayPacket>(player, packet) {
    
    var warningDelay = packet.warningDelay
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetBorderWarningDelayPacket =
        ClientboundSetBorderWarningDelayPacket(warningDelay)
}
