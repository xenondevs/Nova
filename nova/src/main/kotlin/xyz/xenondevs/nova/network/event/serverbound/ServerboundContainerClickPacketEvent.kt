package xyz.xenondevs.nova.network.event.serverbound

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ClickType
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundContainerClickPacketEvent(
    player: Player,
    packet: ServerboundContainerClickPacket
) : PlayerPacketEvent<ServerboundContainerClickPacket>(player, packet) {
    
    var containerId: Int = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    var stateId: Int = packet.stateId
        set(value) {
            field = value
            changed = true
        }
    var slotNum: Int = packet.slotNum.toInt()
        set(value) {
            field = value
            changed = true
        }
    var buttonNum: Int = packet.buttonNum.toInt()
        set(value) {
            field = value
            changed = true
        }
    var carriedItem: HashedStack = packet.carriedItem
        set(value) {
            field = value
            changed = true
        }
    var changedSlots: Int2ObjectMap<HashedStack> = packet.changedSlots
        set(value) {
            field = value
            changed = true
        }
    var clickType: ClickType = packet.clickType
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundContainerClickPacket {
        return ServerboundContainerClickPacket(containerId, stateId, slotNum.toShort(), buttonNum.toByte(), clickType, changedSlots, carriedItem)
    }
    
}