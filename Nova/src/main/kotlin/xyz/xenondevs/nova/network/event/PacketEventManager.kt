package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.clientbound.*
import xyz.xenondevs.nova.network.event.serverbound.PlaceRecipePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.PlayerActionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.SetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.UseItemPacketEvent
import xyz.xenondevs.nova.util.callEvent
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
object PacketEventManager {
    
    private val events = HashMap<KClass<out Packet<*>>, (Packet<*>) -> PacketEvent<Packet<*>>>()
    private val playerEvents = HashMap<KClass<out Packet<*>>, (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>>()
    
    init {
        registerPlayerEventType(ClientboundSystemChatPacket::class, ::SystemChatPacketEvent)
        registerPlayerEventType(ClientboundSetActionBarTextPacket::class, ::ActionBarPacketEvent)
        registerPlayerEventType(ClientboundContainerSetContentPacket::class, ::ContainerSetContentPacketEvent)
        registerPlayerEventType(ClientboundContainerSetSlotPacket::class, ::ContainerSetSlotPacketEvent)
        registerPlayerEventType(ClientboundSetEntityDataPacket::class, ::SetEntityDataPacketEvent)
        registerPlayerEventType(ClientboundSetEquipmentPacket::class, ::SetEquipmentPacketEvent)
        registerPlayerEventType(ClientboundUpdateRecipesPacket::class, ::UpdateRecipesPacketEvent)
        registerPlayerEventType(ClientboundBlockDestructionPacket::class, ::BlockDestructionPacketEvent)
        registerPlayerEventType(ClientboundSoundPacket::class, ::SoundPacketEvent)
        registerPlayerEventType(ClientboundSetPassengersPacket::class, ::SetPassengersPacketEvent)
        registerPlayerEventType(ServerboundPlaceRecipePacket::class, ::PlaceRecipePacketEvent)
        registerPlayerEventType(ServerboundSetCreativeModeSlotPacket::class, ::SetCreativeModeSlotPacketEvent)
        registerPlayerEventType(ServerboundPlayerActionPacket::class, ::PlayerActionPacketEvent)
        registerPlayerEventType(ServerboundUseItemPacket::class, ::UseItemPacketEvent)
    }
    
    private fun <P : Packet<*>> registerEventType(clazz: KClass<out P>, constructor: (P) -> PacketEvent<P>) {
        events[clazz] = constructor as (Packet<*>) -> PacketEvent<Packet<*>>
    }
    
    private fun <P : Packet<*>> registerPlayerEventType(clazz: KClass<out P>, constructor: (Player, P) -> PlayerPacketEvent<P>) {
        playerEvents[clazz] = constructor as (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>
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