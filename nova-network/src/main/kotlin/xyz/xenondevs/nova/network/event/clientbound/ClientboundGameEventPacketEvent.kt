package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundGameEventPacketEvent(
    player: Player,
    packet: ClientboundGameEventPacket
) : PlayerPacketEvent<ClientboundGameEventPacket>(player, packet) {
    
    var event: ClientboundGameEventPacket.Type = packet.event
        set(value) {
            field = value
            changed = true
        }
    
    var param: Float = packet.param
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundGameEventPacket =
        ClientboundGameEventPacket(event, param)
    
}
