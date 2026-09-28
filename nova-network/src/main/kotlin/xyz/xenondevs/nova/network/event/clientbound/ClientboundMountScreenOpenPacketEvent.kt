package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundMountScreenOpenPacketEvent(
    player: Player,
    packet: ClientboundMountScreenOpenPacket
) : PlayerPacketEvent<ClientboundMountScreenOpenPacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    
    var inventoryColumns = packet.inventoryColumns
        set(value) {
            field = value
            changed = true
        }
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundMountScreenOpenPacket =
        ClientboundMountScreenOpenPacket(containerId, inventoryColumns, entityId)
    
}
