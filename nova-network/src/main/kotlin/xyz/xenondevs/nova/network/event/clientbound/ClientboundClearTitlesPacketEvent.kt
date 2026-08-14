package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundClearTitlesPacketEvent(
    player: Player,
    packet: ClientboundClearTitlesPacket
) : PlayerPacketEvent<ClientboundClearTitlesPacket>(player, packet) {
    
    var resetTimes: Boolean = packet.shouldResetTimes()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundClearTitlesPacket =
        ClientboundClearTitlesPacket(resetTimes)
    
}
