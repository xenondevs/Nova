package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundUseItemOnPacketEvent(
    player: Player,
    packet: ServerboundUseItemOnPacket
) : PlayerPacketEvent<ServerboundUseItemOnPacket>(player, packet) {
    
    var hand: InteractionHand = packet.hand
        set(value) {
            field = value
            changed = true
        }
    var hitResult: BlockHitResult = packet.hitResult
        set(value) {
            field = value
            changed = true
        }
    var sequence: Int = packet.sequence
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundUseItemOnPacket {
        return ServerboundUseItemOnPacket(hand, hitResult, sequence)
    }
    
}