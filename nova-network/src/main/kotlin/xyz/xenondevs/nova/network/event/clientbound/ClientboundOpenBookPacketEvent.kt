package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.world.InteractionHand
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundOpenBookPacketEvent(
    player: Player,
    packet: ClientboundOpenBookPacket
) : PlayerPacketEvent<ClientboundOpenBookPacket>(player, packet) {
    
    var hand: InteractionHand = packet.hand
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundOpenBookPacket =
        ClientboundOpenBookPacket(hand)
    
}
