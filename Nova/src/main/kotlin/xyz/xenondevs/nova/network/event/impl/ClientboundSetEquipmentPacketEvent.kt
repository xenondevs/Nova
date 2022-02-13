package xyz.xenondevs.nova.network.event.impl

import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

class ClientboundSetEquipmentPacketEvent(
    player: Player,
    packet: ClientboundSetEquipmentPacket
) : PacketEvent<ClientboundSetEquipmentPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
        
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
    private var changed = false
    
    var slots: List<MojangPair<EquipmentSlot, MojangStack>> by MutableLazy({ changed = true }) {
        super.packet.slots
    }
    
    override val packet: ClientboundSetEquipmentPacket
        get() {
            val original = super.packet
            return if (changed)
                ClientboundSetEquipmentPacket(
                    original.entity,
                    slots
                )
            else original
        }
}