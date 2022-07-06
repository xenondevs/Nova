package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.ClientboundSetEntityDataPacket
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSetEntityDataPacketEvent(
    player: Player,
    packet: ClientboundSetEntityDataPacket
) : PlayerPacketEvent<ClientboundSetEntityDataPacket>(player, packet) {
    
    var id = packet.id
        set(value) {
            field = value
            changed = true
        }
    var unpackedData = packet.unpackedData
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetEntityDataPacket {
        return ClientboundSetEntityDataPacket(id, unpackedData)
    }
    
}