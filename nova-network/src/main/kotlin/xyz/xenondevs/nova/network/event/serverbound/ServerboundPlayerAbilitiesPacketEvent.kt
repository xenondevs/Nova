package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ServerboundPlayerAbilitiesPacket

class ServerboundPlayerAbilitiesPacketEvent(
    player: Player,
    packet: ServerboundPlayerAbilitiesPacket
) : PlayerPacketEvent<ServerboundPlayerAbilitiesPacket>(player, packet) {
    
    var flying = packet.isFlying
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPlayerAbilitiesPacket =
        ServerboundPlayerAbilitiesPacket(flying)
}
