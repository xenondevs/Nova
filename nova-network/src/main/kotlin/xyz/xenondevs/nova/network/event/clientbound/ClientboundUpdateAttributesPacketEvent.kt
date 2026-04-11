package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundUpdateAttributesPacketEvent(
    player: Player,
    packet: ClientboundUpdateAttributesPacket
) : PlayerPacketEvent<ClientboundUpdateAttributesPacket>(player, packet) {
    
    var entityId: Int = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    var values: List<AttributeSnapshot> = packet.values
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundUpdateAttributesPacket {
        return ClientboundUpdateAttributesPacket(entityId, values)
    }
    
}