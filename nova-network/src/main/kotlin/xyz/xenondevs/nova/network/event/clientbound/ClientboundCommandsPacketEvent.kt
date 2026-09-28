package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.packet.ClientboundCommandsPacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundCommandsPacketEvent(
    player: Player,
    packet: ClientboundCommandsPacket
) : PlayerPacketEvent<ClientboundCommandsPacket>(player, packet) {
    
    var rootIndex: Int = packet.rootIndex
        set(value) {
            field = value
            changed = true
        }
    
    var entries: List<ClientboundCommandsPacket.Entry> = packet.entries
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundCommandsPacket {
        return ClientboundCommandsPacket(entries, rootIndex)
    }
    
}
