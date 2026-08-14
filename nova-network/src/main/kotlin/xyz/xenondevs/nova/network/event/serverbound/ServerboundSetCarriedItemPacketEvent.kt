package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetCarriedItemPacketEvent(
    player: Player,
    packet: ServerboundSetCarriedItemPacket
) : PlayerPacketEvent<ServerboundSetCarriedItemPacket>(player, packet) {
    
    var slot = packet.slot
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundSetCarriedItemPacket(slot)
}
