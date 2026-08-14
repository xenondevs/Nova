package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetChunkCacheRadiusPacketEvent(
    player: Player,
    packet: ClientboundSetChunkCacheRadiusPacket
) : PlayerPacketEvent<ClientboundSetChunkCacheRadiusPacket>(player, packet) {
    
    var radius = packet.radius
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSetChunkCacheRadiusPacket(radius)
}
