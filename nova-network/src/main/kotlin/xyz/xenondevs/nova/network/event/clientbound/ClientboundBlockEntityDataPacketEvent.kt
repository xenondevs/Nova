package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundBlockEntityDataPacketEvent(
    player: Player,
    packet: ClientboundBlockEntityDataPacket
) : PlayerPacketEvent<ClientboundBlockEntityDataPacket>(player, packet) {
    
    var pos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var type = packet.type
        set(value) {
            field = value
            changed = true
        }
    var tag = packet.tag
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket(pos, type, tag)
    }
    
}
