package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundLockDifficultyPacketEvent(
    player: Player,
    packet: ServerboundLockDifficultyPacket
) : PlayerPacketEvent<ServerboundLockDifficultyPacket>(player, packet) {
    
    var locked = packet.isLocked
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundLockDifficultyPacket(locked)
}
