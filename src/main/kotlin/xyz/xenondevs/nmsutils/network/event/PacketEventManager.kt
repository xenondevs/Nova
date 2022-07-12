package xyz.xenondevs.nmsutils.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import xyz.xenondevs.nmsutils.network.event.clientbound.*
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlaceRecipePacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundSetCreativeModeSlotPacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundUseItemPacketEvent
import xyz.xenondevs.nmsutils.util.removeIf
import java.lang.reflect.Method
import kotlin.reflect.KClass

private data class Listener(val instance: Any, val method: Method, val priority: EventPriority, val ignoreIfCancelled: Boolean)

@Suppress("UNCHECKED_CAST")
object PacketEventManager {
    
    private val eventTypes = HashMap<KClass<out Packet<*>>, KClass<out PacketEvent<*>>>()
    
    private val eventConstructors = HashMap<KClass<out Packet<*>>, (Packet<*>) -> PacketEvent<Packet<*>>>()
    private val playerEventConstructors = HashMap<KClass<out Packet<*>>, (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>>()
    
    private val listeners = HashMap<KClass<out PacketEvent<*>>, MutableList<Listener>>()
    private val listenerInstances = HashMap<Any, List<Listener>>()
    
    init {
        // clientbound - player
        registerPlayerEventType(::ClientboundSystemChatPacketEvent)
        registerPlayerEventType(::ClientboundActionBarPacketEvent)
        registerPlayerEventType(::ClientboundContainerSetContentPacketEvent)
        registerPlayerEventType(::ClientboundContainerSetSlotPacketEvent)
        registerPlayerEventType(::ClientboundSetEntityDataPacketEvent)
        registerPlayerEventType(::ClientboundSetEquipmentPacketEvent)
        registerPlayerEventType(::ClientboundUpdateRecipesPacketEvent)
        registerPlayerEventType(::ClientboundBlockDestructionPacketEvent)
        registerPlayerEventType(::ClientboundSoundPacketEvent)
        registerPlayerEventType(::ClientboundSetPassengersPacketEvent)
        registerPlayerEventType(::ClientboundLevelChunkWithLightPacketEvent)
        registerPlayerEventType(::ClientboundBlockUpdatePacketEvent)
        registerPlayerEventType(::ClientboundBlockEventPacketEvent)
        
        // serverbound - player
        registerPlayerEventType(::ServerboundPlaceRecipePacketEvent)
        registerPlayerEventType(::ServerboundSetCreativeModeSlotPacketEvent)
        registerPlayerEventType(::ServerboundPlayerActionPacketEvent)
        registerPlayerEventType(::ServerboundUseItemPacketEvent)
    }
    
    private inline fun <reified P : Packet<*>, reified E : PlayerPacketEvent<P>> registerEventType(noinline constructor: (P) -> E) {
        eventTypes[P::class] = E::class
        eventConstructors[P::class] = constructor as (Packet<*>) -> PacketEvent<Packet<*>>
    }
    
    private inline fun <reified P : Packet<*>, reified E : PlayerPacketEvent<P>> registerPlayerEventType(noinline constructor: (Player, P) -> E) {
        eventTypes[P::class] = E::class
        playerEventConstructors[P::class] = constructor as (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>
    }
    
    @Synchronized
    internal fun createAndCallEvent(player: Player?, packet: Packet<*>): PacketEvent<*>? {
        val packetClass = packet::class
        
        val packetEventClass = eventTypes[packetClass]
        if (packetEventClass != null && listeners[packetEventClass] != null) {
            val event = playerEventConstructors[packetClass]?.invoke(player ?: return null, packet)
                ?: eventConstructors[packetClass]?.invoke(packet)
                ?: return null
            
            callEvent(event)
            
            return event
        }
        
        return null
    }
    
    @Synchronized
    private fun callEvent(event: PacketEvent<*>) {
        listeners[event::class]?.forEach { (instance, method, _, ignoreIfCancelled) ->
            if (!ignoreIfCancelled || !event.isCancelled) {
                method.invoke(instance, event)
            }
        }
    }
    
    @Synchronized
    fun registerListener(listenerInstance: Any) {
        val instanceListeners = ArrayList<Listener>()
        
        listenerInstance::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(PacketHandler::class.java) && method.parameters.size == 1) {
                val param = method.parameters.first().type.kotlin
                if (param in eventTypes.values) {
                    param as KClass<out PacketEvent<*>>
                    method.isAccessible = true
                    
                    val priority = method.getAnnotation(PacketHandler::class.java).priority
                    val ignoreIfCancelled = method.getAnnotation(PacketHandler::class.java).ignoreIfCancelled
                    
                    val listener = Listener(listenerInstance, method, priority, ignoreIfCancelled)
                    instanceListeners += listener
                    
                    val list = listeners[param]?.let(::ArrayList) ?: ArrayList()
                    list += listener
                    list.sortBy { it.priority }
                    
                    listeners[param] = list
                }
            }
        }
        
        if (instanceListeners.isNotEmpty())
            listenerInstances[listenerInstance] = instanceListeners
    }
    
    @Synchronized
    fun unregisterListener(listener: Any) {
        val toRemove = listenerInstances[listener]?.toHashSet() ?: return
        
        listeners.removeIf { (_, list) ->
            list.removeIf { it in toRemove }
            return@removeIf list.isEmpty()
        }
        
        listenerInstances -= listener
    }
    
}