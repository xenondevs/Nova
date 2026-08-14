package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundClientCommandPacketEvent(
    player: Player,
    packet: ServerboundClientCommandPacket
) : PlayerPacketEvent<ServerboundClientCommandPacket>(player, packet) {
    
    var action: ServerboundClientCommandPacket.Action = packet.action
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundClientCommandPacket(action)
}
