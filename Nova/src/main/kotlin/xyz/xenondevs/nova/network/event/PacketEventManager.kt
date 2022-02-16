package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.clientbound.*
import xyz.xenondevs.nova.network.event.serverbound.PlaceRecipePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.SetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.util.callEvent
import kotlin.reflect.KClass

object PacketEventManager {
    
    private val events = HashMap<KClass<out Packet<*>>, (Packet<*>) -> PacketEvent<Packet<*>>>()
    private val playerEvents = HashMap<KClass<out Packet<*>>, (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>>()
    
    init {
        registerPlayerEventType(ClientboundChatPacket::class, ::ChatPacketEvent)
        registerPlayerEventType(ClientboundSetActionBarTextPacket::class, ::ActionBarPacketEvent)
        registerPlayerEventType(ClientboundContainerSetContentPacket::class, ::ContainerSetContentPacketEvent)
        registerPlayerEventType(ClientboundContainerSetSlotPacket::class, ::ContainerSetSlotPacketEvent)
        registerPlayerEventType(ClientboundSetEntityDataPacket::class, ::SetEntityDataPacketEvent)
        registerPlayerEventType(ClientboundSetEquipmentPacket::class, ::SetEquipmentPacketEvent)
        registerPlayerEventType(ServerboundPlaceRecipePacket::class, ::PlaceRecipePacketEvent)
        registerPlayerEventType(ServerboundSetCreativeModeSlotPacket::class, ::SetCreativeModeSlotPacketEvent)
    }
    
    private fun <P : Packet<*>> registerEventType(clazz: KClass<out P>, constructor: (P) -> PacketEvent<P>) {
        events[clazz] = constructor as (Packet<*>) -> PacketEvent<Packet<*>> // TODO ?
    }
    
    private fun <P : Packet<*>> registerPlayerEventType(clazz: KClass<out P>, constructor: (Player, P) -> PlayerPacketEvent<P>) {
        playerEvents[clazz] = constructor as (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>> // TODO ?
    }
    
    fun createAndCallEvent(player: Player?, packet: Packet<*>): PacketEvent<*>? {
        val packetClass = packet::class
        if (packetClass in events) {
            return events[packetClass]!!.invoke(packet).also(::callEvent)
        } else if (packetClass in playerEvents) {
            if (player == null) return null
            return playerEvents[packetClass]!!.invoke(player, packet).also(::callEvent)
        }
        return null
    }
    
}