package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.NonNullList
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent
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
        val original = super.packet
        return ClientboundContainerSetContentPacket(
            original.containerId,
            original.stateId,
            NonNullList(items, MojangStack.EMPTY),
            carriedItem
        )
    }
    
}