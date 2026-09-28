package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundPlayerActionPacketEvent(
    player: Player,
    packet: ServerboundPlayerActionPacket
) : PlayerPacketEvent<ServerboundPlayerActionPacket>(player, packet) {
    
    var action = packet.action
        set(value) {
            field = value
            changed = true
        }
    var pos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var direction = packet.direction
        set(value) {
            field = value
            changed = true
        }
    var sequence = packet.sequence
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPlayerActionPacket {
        return ServerboundPlayerActionPacket(action, pos, direction, sequence)
    }
    
}