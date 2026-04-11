package xyz.xenondevs.nova.network.event.clientbound

import com.mojang.datafixers.util.Pair
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetEquipmentPacketEvent(
    player: Player,
    packet: ClientboundSetEquipmentPacket
) : PlayerPacketEvent<ClientboundSetEquipmentPacket>(player, packet) {
    
    var entity: Int = packet.entity
    var slots: List<Pair<EquipmentSlot, ItemStack>> = packet.slots
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetEquipmentPacket {
        return ClientboundSetEquipmentPacket(
            entity,
            slots
        )
    }
    
}