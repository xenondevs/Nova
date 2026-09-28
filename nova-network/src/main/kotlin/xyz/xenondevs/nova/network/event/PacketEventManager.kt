@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import xyz.xenondevs.nova.network.event.clientbound.ClientboundAddEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundAnimatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockDestructionPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockEntityDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockUpdatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBossEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBundleDelimiterPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBundlePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundChunkBatchStartPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundClearDialogPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundClearTitlesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundCommandsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerClosePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetSlotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundEntityEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundFinishConfigurationPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundGameEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundHelloPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundInitializeBorderPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundKeepAlivePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelChunkWithLightPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelEventPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelParticlesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLightUpdatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLoginCompressionPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLowDiskSpaceWarningPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMerchantOffersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMountScreenOpenPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMoveEntityPosPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMoveEntityPosRotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMoveEntityRotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenBookPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenScreenPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenSignEditorPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPingPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerAbilitiesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerCombatEndPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerCombatEnterPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerInfoUpdatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerLookAtPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundProjectilePowerPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundRemoveEntitiesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundResetChatPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundRotateHeadPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSectionBlocksUpdatePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSelectAdvancementsTabPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetBorderCenterPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetBorderLerpSizePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetBorderSizePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetBorderWarningDelayPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetBorderWarningDistancePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetCameraPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetChunkCacheCenterPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetChunkCacheRadiusPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetDisplayObjectivePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEntityLinkPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEquipmentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetExperiencePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetHealthPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetObjectivePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetPassengersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetPlayerTeamPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetTitlesAnimationPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundStartConfigurationPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundStopSoundPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundTagQueryPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundTakeItemEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAdvancementsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAttributesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateMobEffectPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateTagsPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAcceptTeleportationPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundBlockEntityTagQueryPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundClientCommandPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundCommandSuggestionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundConfigurationAcknowledgedPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundContainerClosePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundEntityTagQueryPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundFinishConfigurationPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundJigsawGeneratePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundKeepAlivePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundKeyPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundLockDifficultyPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundLoginAcknowledgedPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundMovePlayerPosPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundMovePlayerPosRotPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundMovePlayerRotPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundMovePlayerStatusOnlyPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPaddleBoatPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPingRequestPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerAbilitiesPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerCommandPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPongPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundRecipeBookChangeSettingsPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundRenameItemPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSeenAdvancementsPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSelectTradePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetCarriedItemPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetCommandBlockPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetCommandMinecartPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetJigsawBlockPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetStructureBlockPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSignUpdatePacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundStatusRequestPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPunchPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundTeleportToEntityPacketEvent
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
        registerPlayerEventType(::ClientboundAddEntityPacketEvent)
        registerPlayerEventType(::ClientboundAnimatePacketEvent)
        registerPlayerEventType(::ClientboundBlockDestructionPacketEvent)
        registerPlayerEventType(::ClientboundBlockEntityDataPacketEvent)
        registerPlayerEventType(::ClientboundBlockEventPacketEvent)
        registerPlayerEventType(::ClientboundBlockUpdatePacketEvent)
        registerPlayerEventType(::ClientboundBossEventPacketEvent)
        registerPlayerEventType(::ClientboundBundleDelimiterPacketEvent)
        registerPlayerEventType(::ClientboundBundlePacketEvent)
        registerPlayerEventType(::ClientboundChunkBatchStartPacketEvent)
        registerEventType(::ClientboundClearDialogPacketEvent)
        registerPlayerEventType(::ClientboundClearTitlesPacketEvent)
        registerPlayerEventType(::ClientboundCommandsPacketEvent)
        registerPlayerEventType(::ClientboundContainerClosePacketEvent)
        registerPlayerEventType(::ClientboundContainerSetDataPacketEvent)
        registerPlayerEventType(::ClientboundContainerSetSlotPacketEvent)
        registerPlayerEventType(::ClientboundEntityEventPacketEvent)
        registerEventType(::ClientboundFinishConfigurationPacketEvent)
        registerPlayerEventType(::ClientboundGameEventPacketEvent)
        registerEventType(::ClientboundHelloPacketEvent)
        registerPlayerEventType(::ClientboundInitializeBorderPacketEvent)
        registerEventType(::ClientboundKeepAlivePacketEvent)
        registerPlayerEventType(::ClientboundLevelEventPacketEvent)
        registerEventType(::ClientboundLoginCompressionPacketEvent)
        registerPlayerEventType(::ClientboundLowDiskSpaceWarningPacketEvent)
        registerPlayerEventType(::ClientboundMerchantOffersPacketEvent)
        registerPlayerEventType(::ClientboundMountScreenOpenPacketEvent)
        registerPlayerEventType(::ClientboundMoveEntityPosPacketEvent)
        registerPlayerEventType(::ClientboundMoveEntityPosRotPacketEvent)
        registerPlayerEventType(::ClientboundMoveEntityRotPacketEvent)
        registerPlayerEventType(::ClientboundOpenScreenPacketEvent)
        registerEventType(::ClientboundPingPacketEvent)
        registerPlayerEventType(::ClientboundPlayerAbilitiesPacketEvent)
        registerPlayerEventType(::ClientboundPlayerCombatEndPacketEvent)
        registerPlayerEventType(::ClientboundPlayerCombatEnterPacketEvent)
        registerPlayerEventType(::ClientboundPlayerInfoUpdatePacketEvent)
        registerPlayerEventType(::ClientboundPlayerLookAtPacketEvent)
        registerPlayerEventType(::ClientboundProjectilePowerPacketEvent)
        registerEventType(::ClientboundResetChatPacketEvent)
        registerPlayerEventType(::ClientboundRotateHeadPacketEvent)
        registerPlayerEventType(::ClientboundSectionBlocksUpdatePacketEvent)
        registerPlayerEventType(::ClientboundSelectAdvancementsTabPacketEvent)
        registerPlayerEventType(::ClientboundSetBorderCenterPacketEvent)
        registerPlayerEventType(::ClientboundSetBorderLerpSizePacketEvent)
        registerPlayerEventType(::ClientboundSetBorderSizePacketEvent)
        registerPlayerEventType(::ClientboundSetBorderWarningDelayPacketEvent)
        registerPlayerEventType(::ClientboundSetBorderWarningDistancePacketEvent)
        registerPlayerEventType(::ClientboundSetCameraPacketEvent)
        registerPlayerEventType(::ClientboundSetChunkCacheCenterPacketEvent)
        registerPlayerEventType(::ClientboundSetChunkCacheRadiusPacketEvent)
        registerPlayerEventType(::ClientboundSetDisplayObjectivePacketEvent)
        registerPlayerEventType(::ClientboundSetEntityLinkPacketEvent)
        registerPlayerEventType(::ClientboundSetEquipmentPacketEvent)
        registerPlayerEventType(::ClientboundSetExperiencePacketEvent)
        registerPlayerEventType(::ClientboundSetHealthPacketEvent)
        registerPlayerEventType(::ClientboundSetObjectivePacketEvent)
        registerPlayerEventType(::ClientboundSetPassengersPacketEvent)
        registerPlayerEventType(::ClientboundSetPlayerTeamPacketEvent)
        registerPlayerEventType(::ClientboundSetTitlesAnimationPacketEvent)
        registerPlayerEventType(::ClientboundSoundEntityPacketEvent)
        registerPlayerEventType(::ClientboundSoundPacketEvent)
        registerPlayerEventType(::ClientboundStartConfigurationPacketEvent)
        registerPlayerEventType(::ClientboundStopSoundPacketEvent)
        registerPlayerEventType(::ClientboundTagQueryPacketEvent)
        registerPlayerEventType(::ClientboundTakeItemEntityPacketEvent)
        registerPlayerEventType(::ClientboundUpdateAttributesPacketEvent)
        registerPlayerEventType(::ClientboundUpdateMobEffectPacketEvent)
        registerPlayerEventType(::ServerboundBlockEntityTagQueryPacketEvent)
        registerPlayerEventType(::ServerboundClientCommandPacketEvent)
        registerPlayerEventType(::ServerboundConfigurationAcknowledgedPacketEvent)
        registerPlayerEventType(::ServerboundContainerClosePacketEvent)
        registerPlayerEventType(::ServerboundEntityTagQueryPacketEvent)
        registerEventType(::ServerboundFinishConfigurationPacketEvent)
        registerPlayerEventType(::ServerboundJigsawGeneratePacketEvent)
        registerEventType(::ServerboundKeepAlivePacketEvent)
        registerEventType(::ServerboundKeyPacketEvent)
        registerPlayerEventType(::ServerboundLockDifficultyPacketEvent)
        registerEventType(::ServerboundLoginAcknowledgedPacketEvent)
        registerPlayerEventType(::ServerboundMovePlayerPosPacketEvent)
        registerPlayerEventType(::ServerboundMovePlayerPosRotPacketEvent)
        registerPlayerEventType(::ServerboundMovePlayerRotPacketEvent)
        registerPlayerEventType(::ServerboundMovePlayerStatusOnlyPacketEvent)
        registerPlayerEventType(::ServerboundPaddleBoatPacketEvent)
        registerEventType(::ServerboundPingRequestPacketEvent)
        registerPlayerEventType(::ServerboundPlayerAbilitiesPacketEvent)
        registerPlayerEventType(::ServerboundPlayerActionPacketEvent)
        registerPlayerEventType(::ServerboundPlayerCommandPacketEvent)
        registerEventType(::ServerboundPongPacketEvent)
        registerPlayerEventType(::ServerboundRecipeBookChangeSettingsPacketEvent)
        registerPlayerEventType(::ServerboundRenameItemPacketEvent)
        registerPlayerEventType(::ServerboundSeenAdvancementsPacketEvent)
        registerPlayerEventType(::ServerboundSelectTradePacketEvent)
        registerPlayerEventType(::ServerboundSetCarriedItemPacketEvent)
        registerPlayerEventType(::ServerboundSetCommandBlockPacketEvent)
        registerPlayerEventType(::ServerboundSetCommandMinecartPacketEvent)
        registerPlayerEventType(::ServerboundSetJigsawBlockPacketEvent)
        registerPlayerEventType(::ServerboundSetStructureBlockPacketEvent)
        registerEventType(::ServerboundStatusRequestPacketEvent)
        registerPlayerEventType(::ServerboundTeleportToEntityPacketEvent)
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
        listeners[event::class]?.forEach { (handle, ignoreIfCancelled) ->
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
        
        listeners.entries.removeIf { [_, list] ->
            list.removeIf { it in toRemove }
            return@removeIf list.isEmpty()
        }
        
        listenerInstances -= listener
    }
    
}
