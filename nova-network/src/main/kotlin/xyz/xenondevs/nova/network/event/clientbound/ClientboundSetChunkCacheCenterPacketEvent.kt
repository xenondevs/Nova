package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetChunkCacheCenterPacketEvent(
    player: Player,
    packet: ClientboundSetChunkCacheCenterPacket
) : PlayerPacketEvent<ClientboundSetChunkCacheCenterPacket>(player, packet) {
    
    var x = packet.x
        set(value) {
            field = value
            changed = true
        }
    
    var z = packet.z
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSetChunkCacheCenterPacket(x, z)
}
