@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockDestructionPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockUpdatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBossEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetSlotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelChunkWithLightPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMerchantOffersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenScreenPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEquipmentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetPassengersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAdvancementsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAttributesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateTagsPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSwingPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundUseItemOnPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundUseItemPacketEvent
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import net.minecraft.network.PacketListener as MojangPacketListener

private data class Listener(val handle: MethodHandle, val priority: EventPriority, val ignoreIfCancelled: Boolean)

internal object PacketEventManager {
    
    private val LOCK = ReentrantLock()
    
    private val eventTypes = HashMap<KClass<out Packet<*>>, KClass<out PacketEvent<*>>>()
    private val eventConstructors = HashMap<KClass<out Packet<*>>, (Packet<*>) -> PacketEvent<Packet<*>>>()
    private val playerEventConstructors = HashMap<KClass<out Packet<*>>, (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>>()
    
    private val listeners = HashMap<KClass<out PacketEvent<*>>, MutableList<Listener>>()
    private val listenerInstances = HashMap<Any, List<Listener>>()
    
    init {
        // generated events for all record-based packets
        registerGeneratedPacketEvents()
        
        // handwritten events for non-record packets
        registerEventType(::ClientboundUpdateTagsPacketEvent)
        registerPlayerEventType(::ClientboundBlockDestructionPacketEvent)
        registerPlayerEventType(::ClientboundBlockEventPacketEvent)
        registerPlayerEventType(::ClientboundBlockUpdatePacketEvent)
        registerPlayerEventType(::ClientboundBossEventPacketEvent)
        registerPlayerEventType(::ClientboundContainerSetDataPacketEvent)
        registerPlayerEventType(::ClientboundContainerSetSlotPacketEvent)
        registerPlayerEventType(::ClientboundLevelChunkWithLightPacketEvent)
        registerPlayerEventType(::ClientboundLevelEventPacketEvent)
        registerPlayerEventType(::ClientboundMerchantOffersPacketEvent)
        registerPlayerEventType(::ClientboundOpenScreenPacketEvent)
        registerPlayerEventType(::ClientboundSetEquipmentPacketEvent)
        registerPlayerEventType(::ClientboundSetPassengersPacketEvent)
        registerPlayerEventType(::ClientboundSoundEntityPacketEvent)
        registerPlayerEventType(::ClientboundSoundPacketEvent)
        registerPlayerEventType(::ClientboundUpdateAdvancementsPacketEvent)
        registerPlayerEventType(::ClientboundUpdateAttributesPacketEvent)
        registerPlayerEventType(::ServerboundPlayerActionPacketEvent)
        registerPlayerEventType(::ServerboundSwingPacketEvent)
        registerPlayerEventType(::ServerboundUseItemOnPacketEvent)
        registerPlayerEventType(::ServerboundUseItemPacketEvent)
    }
    
    internal inline fun <reified P : Packet<*>, reified E : PacketEvent<P>> registerEventType(noinline constructor: (P) -> E) {
        eventTypes[P::class] = E::class
        eventConstructors[P::class] = constructor as (Packet<*>) -> PacketEvent<Packet<*>>
    }
    
    internal inline fun <reified P : Packet<*>, reified E : PlayerPacketEvent<P>> registerPlayerEventType(noinline constructor: (Player, P) -> E) {
        eventTypes[P::class] = E::class
        playerEventConstructors[P::class] = constructor as (Player, Packet<*>) -> PlayerPacketEvent<Packet<*>>
    }
    
    fun <T : MojangPacketListener, P : Packet<in T>> createAndCallEvent(player: Player?, packet: P): PacketEvent<P>? = LOCK.withLock {
        val packetClass = packet::class
        
        val packetEventClass = eventTypes[packetClass]
        if (packetEventClass != null && listeners[packetEventClass] != null) {
            val event = playerEventConstructors[packetClass]?.invoke(player ?: return null, packet)
                ?: eventConstructors[packetClass]?.invoke(packet)
                ?: return null
            
            callEvent(event)
            
            return event as PacketEvent<P>
        }
        
        return null
    }
    
    private fun callEvent(event: PacketEvent<*>) {
        listeners[event::class]?.forEach { (handle, _, ignoreIfCancelled) ->
            if (!ignoreIfCancelled || !event.isCancelled) {
                try {
                    handle.invoke(event)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }
    
    fun registerListener(listener: PacketListener): Unit = LOCK.withLock {
        val instanceListeners = ArrayList<Listener>()
        
        listener::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(PacketHandler::class.java) && method.parameters.size == 1) {
                val param = method.parameters.first().type.kotlin
                if (param in eventTypes.values) {
                    param as KClass<out PacketEvent<*>>
                    method.isAccessible = true
                    
                    val priority = method.getAnnotation(PacketHandler::class.java).priority
                    val ignoreIfCancelled = method.getAnnotation(PacketHandler::class.java).ignoreIfCancelled
                    
                    val methodHandle = MethodHandles.lookup().unreflect(method).bindTo(listener)
                    val listener = Listener(methodHandle, priority, ignoreIfCancelled)
                    instanceListeners += listener
                    
                    val list = listeners[param]?.let(::ArrayList) ?: ArrayList()
                    list += listener
                    list.sortBy { it.priority }
                    
                    listeners[param] = list
                }
            }
        }
        
        if (instanceListeners.isNotEmpty())
            listenerInstances[listener] = instanceListeners
    }
    
    fun unregisterListener(listener: PacketListener): Unit = LOCK.withLock {
        val toRemove = listenerInstances[listener]?.toHashSet() ?: return
        
        listeners.entries.removeIf { (_, list) ->
            list.removeIf { it in toRemove }
            return@removeIf list.isEmpty()
        }
        
        listenerInstances -= listener
    }
    
}