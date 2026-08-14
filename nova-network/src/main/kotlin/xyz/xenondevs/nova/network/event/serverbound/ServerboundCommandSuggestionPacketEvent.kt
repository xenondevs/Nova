package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundCommandSuggestionPacketEvent(
    player: Player,
    packet: ServerboundCommandSuggestionPacket
) : PlayerPacketEvent<ServerboundCommandSuggestionPacket>(player, packet) {
    
    var id = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    var command: String = packet.command
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundCommandSuggestionPacket(id, command)
}
