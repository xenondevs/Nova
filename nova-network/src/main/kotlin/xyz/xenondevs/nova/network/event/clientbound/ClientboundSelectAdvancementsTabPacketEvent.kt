package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket
import net.minecraft.resources.Identifier
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSelectAdvancementsTabPacketEvent(
    player: Player,
    packet: ClientboundSelectAdvancementsTabPacket
) : PlayerPacketEvent<ClientboundSelectAdvancementsTabPacket>(player, packet) {
    
    var tab: Identifier? = packet.tab
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSelectAdvancementsTabPacket(tab)
}
