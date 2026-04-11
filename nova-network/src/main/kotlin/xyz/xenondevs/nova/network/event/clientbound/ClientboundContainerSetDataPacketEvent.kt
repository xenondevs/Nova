package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundContainerSetDataPacketEvent(
    player: Player,
    packet: ClientboundContainerSetDataPacket
) : PlayerPacketEvent<ClientboundContainerSetDataPacket>(player, packet) {
    
    var containerId: Int = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    var id: Int = packet.id
        set(value) {
            field = value
            changed = true
        }
    var value: Int = packet.value
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundContainerSetDataPacket {
        return ClientboundContainerSetDataPacket(containerId, id, value)
    }
    
}