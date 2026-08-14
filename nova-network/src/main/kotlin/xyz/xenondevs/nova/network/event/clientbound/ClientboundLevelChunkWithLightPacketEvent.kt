package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.packet.ClientboundLevelChunkWithLightPacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundLevelChunkWithLightPacketEvent(
    player: Player,
    packet: ClientboundLevelChunkWithLightPacket
) : PlayerPacketEvent<ClientboundLevelChunkWithLightPacket>(player, packet) {
    
    var x: Int = packet.x
        set(value) {
            field = value
            changed = true
        }
    
    var z: Int = packet.z
        set(value) {
            field = value
            changed = true
        }
    
    var chunkData: ClientboundLevelChunkPacketData = packet.chunkData
        set(value) {
            field = value
            changed = true
        }
    
    var lightData: ClientboundLightUpdatePacketData = packet.lightData
        set(value) {
            field = value
            changed = true
        }
    
    private val ready = packet.isReady
    
    override fun buildChangedPacket(): ClientboundLevelChunkWithLightPacket {
        return ClientboundLevelChunkWithLightPacket(x, z, chunkData, lightData, ready)
    }
    
}
