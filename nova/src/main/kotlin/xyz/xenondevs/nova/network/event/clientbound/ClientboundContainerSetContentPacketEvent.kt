package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.NonNullList
import net.minecraft.world.item.ItemStack as MojangStack

class ClientboundContainerSetContentPacketEvent(
    player: Player,
    packet: ClientboundContainerSetContentPacket
) : PlayerPacketEvent<ClientboundContainerSetContentPacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    var stateId = packet.stateId
        set(value) {
            field = value
            changed = true
        }
    var items = packet.items
        set(value) {
            field = value
            changed = true
        }
    var carriedItem = packet.carriedItem
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundContainerSetContentPacket {
        return ClientboundContainerSetContentPacket(
            containerId,
            stateId,
            NonNullList(items, MojangStack.EMPTY),
            carriedItem
        )
    }
    
}