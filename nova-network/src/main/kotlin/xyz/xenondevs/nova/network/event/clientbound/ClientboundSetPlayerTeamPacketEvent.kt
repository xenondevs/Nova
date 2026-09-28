package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.util.Optional

class ClientboundSetPlayerTeamPacketEvent(
    player: Player,
    packet: ClientboundSetPlayerTeamPacket
) : PlayerPacketEvent<ClientboundSetPlayerTeamPacket>(player, packet) {
    
    var method = packet.method
        set(value) {
            field = value
            changed = true
        }
    
    var name: String = packet.name
        set(value) {
            field = value
            changed = true
        }
    
    var players: Collection<String> = packet.players
        set(value) {
            field = value
            changed = true
        }
    
    var parameters: Optional<ClientboundSetPlayerTeamPacket.Parameters> = packet.parameters
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetPlayerTeamPacket =
        ClientboundSetPlayerTeamPacket(name, method, parameters, players)
}
