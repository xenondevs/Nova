package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSelectBundleItemPacketEvent(
    player: Player,
    packet: ServerboundSelectBundleItemPacket
) : PlayerPacketEvent<ServerboundSelectBundleItemPacket>(player, packet) {
    
    var slotId: Int = packet.slotId
        set(value) {
            field = value
            changed = true
        }
    var selectedItemIndex: Int = packet.selectedItemIndex
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSelectBundleItemPacket {
        return ServerboundSelectBundleItemPacket(slotId, selectedItemIndex)
    }
    

}