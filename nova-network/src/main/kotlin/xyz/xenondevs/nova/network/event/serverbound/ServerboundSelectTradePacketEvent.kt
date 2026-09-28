package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSelectTradePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSelectTradePacketEvent(
    player: Player,
    packet: ServerboundSelectTradePacket
) : PlayerPacketEvent<ServerboundSelectTradePacket>(player, packet) {
    
    var item = packet.item
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundSelectTradePacket(item)
}
