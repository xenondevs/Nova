package xyz.xenondevs.nova.network.event.clientbound

import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundRemoveEntitiesPacketEvent(
    player: Player,
    packet: ClientboundRemoveEntitiesPacket
) : PlayerPacketEvent<ClientboundRemoveEntitiesPacket>(player, packet) {
    
    var entityIds: IntList = packet.entityIds
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundRemoveEntitiesPacket =
        ClientboundRemoveEntitiesPacket(entityIds)
    
}
