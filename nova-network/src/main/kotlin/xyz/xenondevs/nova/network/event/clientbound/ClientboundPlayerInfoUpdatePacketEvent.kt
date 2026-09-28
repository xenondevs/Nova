package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.util.EnumSet

class ClientboundPlayerInfoUpdatePacketEvent(
    player: Player,
    packet: ClientboundPlayerInfoUpdatePacket
) : PlayerPacketEvent<ClientboundPlayerInfoUpdatePacket>(player, packet) {
    
    var actions: EnumSet<ClientboundPlayerInfoUpdatePacket.Action> = packet.actions()
        set(value) {
            field = value
            changed = true
        }
    
    var entries: List<ClientboundPlayerInfoUpdatePacket.Entry> = packet.entries()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundPlayerInfoUpdatePacket =
        ClientboundPlayerInfoUpdatePacket(actions, entries)
    
}
