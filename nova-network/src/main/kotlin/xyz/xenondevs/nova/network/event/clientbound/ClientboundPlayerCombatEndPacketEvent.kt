package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundPlayerCombatEndPacketEvent(
    player: Player,
    packet: ClientboundPlayerCombatEndPacket
) : PlayerPacketEvent<ClientboundPlayerCombatEndPacket>(player, packet) {
    
    var duration = packet.duration
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundPlayerCombatEndPacket =
        ClientboundPlayerCombatEndPacket(duration)
    
}
