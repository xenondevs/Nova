package xyz.xenondevs.nova.network.event.serverbound

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
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
    var slotNum: Int = packet.slotNum
        set(value) {
            field = value
            changed = true
        }
    var buttonNum: Int = packet.buttonNum
        set(value) {
            field = value
            changed = true
        }
    var carriedItem: ItemStack = packet.carriedItem
        set(value) {
            field = value
            changed = true
        }
    var changedSlots: Int2ObjectMap<ItemStack> = packet.changedSlots
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
        return ServerboundContainerClickPacket(containerId, stateId, slotNum, buttonNum, clickType, carriedItem, changedSlots)
    }
    
}