package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundProjectilePowerPacketEvent(
    player: Player,
    packet: ClientboundProjectilePowerPacket
) : PlayerPacketEvent<ClientboundProjectilePowerPacket>(player, packet) {
    
    var id = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    var accelerationPower = packet.accelerationPower
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundProjectilePowerPacket =
        ClientboundProjectilePowerPacket(id, accelerationPower)
    
}
