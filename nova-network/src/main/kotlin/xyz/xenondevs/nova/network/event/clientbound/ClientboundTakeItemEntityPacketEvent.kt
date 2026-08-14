package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundTakeItemEntityPacketEvent(
    player: Player,
    packet: ClientboundTakeItemEntityPacket
) : PlayerPacketEvent<ClientboundTakeItemEntityPacket>(player, packet) {
    
    var itemId = packet.itemId
        set(value) {
            field = value
            changed = true
        }
    
    var playerId = packet.playerId
        set(value) {
            field = value
            changed = true
        }
    
    var amount = packet.amount
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundTakeItemEntityPacket(itemId, playerId, amount)
}
