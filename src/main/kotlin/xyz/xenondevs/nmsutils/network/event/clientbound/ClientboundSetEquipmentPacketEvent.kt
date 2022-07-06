package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSetEquipmentPacketEvent(
    player: Player,
    packet: ClientboundSetEquipmentPacket
) : PlayerPacketEvent<ClientboundSetEquipmentPacket>(player, packet) {
    
    var entity = packet.entity
    var slots = packet.slots
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetEquipmentPacket {
        return ClientboundSetEquipmentPacket(
            entity,
            slots
        )
    }
    
}