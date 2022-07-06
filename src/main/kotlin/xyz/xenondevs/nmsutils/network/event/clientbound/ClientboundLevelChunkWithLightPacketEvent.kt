package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundLevelChunkWithLightPacketEvent(
    player: Player,
    packet: ClientboundLevelChunkWithLightPacket
) : PlayerPacketEvent<ClientboundLevelChunkWithLightPacket>(player, packet) {
    
    val x = packet.x
    val z = packet.z
    val chunkData = packet.chunkData
    val lightData = packet.lightData
    
}