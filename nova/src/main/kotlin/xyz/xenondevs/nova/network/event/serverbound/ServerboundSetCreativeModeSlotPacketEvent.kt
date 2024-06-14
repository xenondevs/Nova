package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetCreativeModeSlotPacketEvent(
    player: Player,
    packet: ServerboundSetCreativeModeSlotPacket
) : PlayerPacketEvent<ServerboundSetCreativeModeSlotPacket>(player, packet) {
    
    var slotNum = packet.slotNum
        set(value) {
            field = value
            changed = true
        }
    var item = packet.item
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSetCreativeModeSlotPacket {
        return ServerboundSetCreativeModeSlotPacket(slotNum, item)
    }
    
}