package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ServerboundPlayerCommandPacket

class ServerboundPlayerCommandPacketEvent(
    player: Player,
    packet: ServerboundPlayerCommandPacket
) : PlayerPacketEvent<ServerboundPlayerCommandPacket>(player, packet) {
    
    var id = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    var action: ServerboundPlayerCommandPacket.Action = packet.action
        set(value) {
            field = value
            changed = true
        }
    
    var data = packet.data
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPlayerCommandPacket =
        ServerboundPlayerCommandPacket(id, action, data)
}
