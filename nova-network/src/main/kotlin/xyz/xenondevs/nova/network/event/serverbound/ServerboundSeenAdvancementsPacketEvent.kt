package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
import net.minecraft.resources.Identifier
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSeenAdvancementsPacketEvent(
    player: Player,
    packet: ServerboundSeenAdvancementsPacket
) : PlayerPacketEvent<ServerboundSeenAdvancementsPacket>(player, packet) {
    
    var action: ServerboundSeenAdvancementsPacket.Action = packet.action
        set(value) {
            field = value
            changed = true
        }
    
    var tab: Identifier? = packet.tab
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundSeenAdvancementsPacket(action, tab)
}
