package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetHealthPacketEvent(
    player: Player,
    packet: ClientboundSetHealthPacket
) : PlayerPacketEvent<ClientboundSetHealthPacket>(player, packet) {
    
    var health = packet.health
        set(value) {
            field = value
            changed = true
        }
    
    var food = packet.food
        set(value) {
            field = value
            changed = true
        }
    
    var saturation = packet.saturation
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSetHealthPacket(health, food, saturation)
}
