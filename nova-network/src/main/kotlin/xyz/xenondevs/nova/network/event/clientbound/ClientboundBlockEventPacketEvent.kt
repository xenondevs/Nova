package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBlockEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundBlockEventPacketEvent(
    player: Player,
    packet: ClientboundBlockEventPacket
) : PlayerPacketEvent<ClientboundBlockEventPacket>(player, packet) {
    
    var pos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var block = packet.block
        set(value) {
            field = value
            changed = true
        }
    var actionId = packet.b0
        set(value) {
            field = value
            changed = true
        }
    var actionParam = packet.b1
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBlockEventPacket {
        return ClientboundBlockEventPacket(pos, block, actionId, actionParam)
    }
    
}