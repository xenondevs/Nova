package xyz.xenondevs.nmsutils.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ServerboundUseItemPacketEvent(
    player: Player,
    packet: ServerboundUseItemPacket
) : PlayerPacketEvent<ServerboundUseItemPacket>(player, packet) {
    
    var hand = packet.hand
        set(value) {
            field = value
            changed = true
        }
    var sequence = packet.sequence
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundUseItemPacket {
        return ServerboundUseItemPacket(hand, sequence)
    }
    
}