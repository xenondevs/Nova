package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetTitlesAnimationPacketEvent(
    player: Player,
    packet: ClientboundSetTitlesAnimationPacket
) : PlayerPacketEvent<ClientboundSetTitlesAnimationPacket>(player, packet) {
    
    var fadeIn = packet.fadeIn
        set(value) {
            field = value
            changed = true
        }
    
    var stay = packet.stay
        set(value) {
            field = value
            changed = true
        }
    
    var fadeOut = packet.fadeOut
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut)
}
