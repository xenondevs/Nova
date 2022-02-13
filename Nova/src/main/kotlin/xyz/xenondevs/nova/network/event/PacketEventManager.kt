package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.impl.*
import kotlin.reflect.KClass

object PacketEventManager {
    
    private val events = HashMap<KClass<out Packet<*>>, (Player, Packet<*>) -> PacketEvent<Packet<*>>>()
    
    init {
        registerEventType(ClientboundChatPacket::class, ::ClientboundChatPacketEvent)
        registerEventType(ClientboundSetActionBarTextPacket::class, ::ClientboundActionBarPacketEvent)
        registerEventType(ClientboundContainerSetContentPacket::class, ::ClientboundContainerSetContentPacketEvent)
        registerEventType(ClientboundContainerSetSlotPacket::class, ::ClientboundContainerSetSlotPacketEvent)
        registerEventType(ClientboundSetEntityDataPacket::class, ::ClientboundSetEntityDataPacketEvent)
        registerEventType(ClientboundSetEquipmentPacket::class, ::ClientboundSetEquipmentPacketEvent)
        registerEventType(ServerboundPlaceRecipePacket::class, ::ServerboundPlaceRecipePacketEvent)
        registerEventType(ServerboundSetCreativeModeSlotPacket::class, ::ServerboundSetCreativeModeSlotPacketEvent)
    }
    
    private fun <P : Packet<*>> registerEventType(clazz: KClass<out P>, constructor: (Player, P) -> PacketEvent<P>) {
        events[clazz] = constructor as (Player, Packet<*>) -> PacketEvent<Packet<*>> // TODO ?
    }
    
    fun createAndCallEvent(player: Player, packet: Packet<*>): PacketEvent<*>? {
        return events[packet::class]
            ?.invoke(player, packet)
            ?.also { Bukkit.getServer().pluginManager.callEvent(it) }
    }
    
}