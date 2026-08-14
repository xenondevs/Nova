package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundLightUpdatePacket

class ClientboundLightUpdatePacketEvent(
    player: Player,
    packet: ClientboundLightUpdatePacket
) : PlayerPacketEvent<ClientboundLightUpdatePacket>(player, packet) {
    
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
    
    var lightData: ClientboundLightUpdatePacketData = packet.lightData
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundLightUpdatePacket =
        ClientboundLightUpdatePacket(x, z, lightData)
    
}
