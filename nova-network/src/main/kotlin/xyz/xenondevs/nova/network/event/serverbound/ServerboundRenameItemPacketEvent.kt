package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundRenameItemPacketEvent(
    player: Player,
    packet: ServerboundRenameItemPacket
) : PlayerPacketEvent<ServerboundRenameItemPacket>(player, packet) {
    
    var name: String = packet.name
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundRenameItemPacket(name)
}
