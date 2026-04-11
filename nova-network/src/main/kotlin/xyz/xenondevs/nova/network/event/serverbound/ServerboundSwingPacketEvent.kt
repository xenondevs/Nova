package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSwingPacketEvent(
    player: Player,
    packet: ServerboundSwingPacket
) : PlayerPacketEvent<ServerboundSwingPacket>(player, packet) {
    
    var hand: InteractionHand = packet.hand
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSwingPacket {
        return ServerboundSwingPacket(hand)
    }
    
}