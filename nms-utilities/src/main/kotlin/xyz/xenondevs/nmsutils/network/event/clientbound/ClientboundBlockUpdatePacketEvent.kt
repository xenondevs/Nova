package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundBlockUpdatePacketEvent(
    player: Player,
    packet: ClientboundBlockUpdatePacket
) : PlayerPacketEvent<ClientboundBlockUpdatePacket>(player, packet) {
    
    var pos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var blockState = packet.blockState
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBlockUpdatePacket {
        return ClientboundBlockUpdatePacket(pos, blockState)
    }
    
}