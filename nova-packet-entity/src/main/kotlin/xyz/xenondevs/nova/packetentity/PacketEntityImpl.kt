package xyz.xenondevs.nova.packetentity

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.joml.Vector3dc
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAttackPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.world.InteractionResult
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val DIRTY_LOCATION = 1
private const val DIRTY_EQUIPMENT = 1 shl 1
private const val DIRTY_VIEWERS = 1 shl 2
private const val DIRTY_ATTRIBUTES = 1 shl 3

internal open class PacketEntityNodeImpl<M : EntityMetadata>(
    private val type: EntityType<*>,
    protected val state: PacketEntityState<*>,
    final override val metadata: M,
    world: World
) : PacketEntityNode<M> {
    
    final override val id: Int = (world as CraftWorld).handle.nextEntityId
    final override val uuid: UUID = UUID.randomUUID()
    final override val equipment: PacketEntityEquipment = PacketEntityEquipmentImpl(state.equipment)
    private val passengerNodes = state.passengers.map { it.createNode(world) }
    final override val passengers: List<PacketEntityNode<*>> = passengerNodes
    private val attackHandlers = state.attackHandlers
    private val attackAsyncHandlers = state.attackAsyncHandlers
    private val interactHandlers = state.interactHandlers
    private val interactAsyncHandlers = state.interactAsyncHandlers
    val graphEntities: List<PacketEntityNodeImpl<*>> = buildList {
        collectGraphEntities(this)
    }
    private val passengerIds = passengerNodes.mapToIntArray(PacketEntityNodeImpl<*>::id)
    val passengersPacket: ClientboundSetPassengersPacket = ClientboundSetPassengersPacket(id, passengerIds)
    
    private val dirtyFlags = AtomicInteger(0)
    private val dirtyMetadata = AtomicLong(0)
    private val flushQueued = AtomicBoolean(false)
    private var observed = false
    
    init {
        // if the limit is ever exceeded, dirtyMetadata needs to be made a bit set
        require(state.components.size <= 64)
    }
    
    open fun observe(root: PacketEntityImpl<*>) {
        if (observed)
            return
        
        observed = true
        state.observe(
            attributesObserver = { if (markDirty(DIRTY_ATTRIBUTES)) root.queueFlush() },
            equipmentObserver = { if (markDirty(DIRTY_EQUIPMENT)) root.queueFlush() },
            metadataObserver = { if (markMetadataDirty(it)) root.queueFlush() }
        )
        passengerNodes.forEach { it.observe(root) }
    }
    
    open fun unobserve() {
        if (!observed)
            return
        
        observed = false
        state.unobserve()
        passengerNodes.forEach { it.unobserve() }
    }
    
    protected fun markDirty(flag: Int): Boolean {
        dirtyFlags.getAndUpdate { it or flag }
        return flushQueued.compareAndSet(false, true)
    }
    
    private fun markMetadataDirty(i: Int): Boolean {
        dirtyMetadata.getAndUpdate { it or (1L shl i) }
        return flushQueued.compareAndSet(false, true)
    }
    
    protected fun dirtyFlags(): Int =
        dirtyFlags.get()
    
    fun flushNode(handledFlags: Int = 0): List<Packet<*>> {
        flushQueued.set(false)
        val dirtyFlags = dirtyFlags.getAndSet(0) and handledFlags.inv()
        val dirtyMetadata = dirtyMetadata.getAndSet(0)
        
        return buildList {
            if ((dirtyFlags and DIRTY_EQUIPMENT) != 0) {
                val packet = equipmentPacket
                if (packet != null)
                    add(packet)
            }
            if ((dirtyFlags and DIRTY_ATTRIBUTES) != 0) {
                val packet = dirtyAttributesPacket
                if (packet != null)
                    add(packet)
            }
            
            var dataValues: MutableList<SynchedEntityData.DataValue<*>>? = null
            for (i in state.components.indices) {
                if ((dirtyMetadata and (1L shl i)) == 0L)
                    continue
                if (dataValues == null)
                    dataValues = mutableListOf()
                
                dataValues += state.components[i].captureAsDataValue(i)
            }
            if (dataValues != null)
                add(ClientboundSetEntityDataPacket(id, dataValues))
        }
    }
    
    private fun addPacket(location: Location): ClientboundAddEntityPacket =
        ClientboundAddEntityPacket(
            id, uuid,
            location.x, location.y, location.z,
            location.pitch, location.yaw,
            type,
            0,
            Vec3.ZERO,
            location.yaw.toDouble()
        )
    
    private val equipmentPacket: ClientboundSetEquipmentPacket?
        get() = state.buildEquipmentPacket(id)
    
    private val fullAttributesPacket: ClientboundUpdateAttributesPacket?
        get() = state.buildFullAttributesPacket(id)
    
    private val dirtyAttributesPacket: ClientboundUpdateAttributesPacket?
        get() = state.buildDirtyAttributesPacket(id)
    
    private val fullMetadataPacket: ClientboundSetEntityDataPacket
        get() = ClientboundSetEntityDataPacket(id, state.components.mapIndexed { i, component -> component.captureAsDataValue(i) })
    
    fun spawnPackets(location: Location): List<Packet<in ClientGamePacketListener>> =
        buildList {
            add(addPacket(location))
            equipmentPacket?.let(::add)
            fullAttributesPacket?.let(::add)
            add(fullMetadataPacket)
        }
    
    private fun collectGraphEntities(entities: MutableList<PacketEntityNodeImpl<*>>) {
        passengerNodes.forEach { it.collectGraphEntities(entities) }
        entities += this
    }
    
    fun runAttackHandlers(player: Player) {
        if (attackHandlers.isEmpty())
            return
        
        val dsl = AttackDslImpl(player)
        for (handler in attackHandlers) {
            try {
                dsl.handler()
            } catch (e: Exception) {
                PacketEntityManager.logger.error("Exception in PacketEntity attack handler", e)
            }
        }
    }
    
    fun runAttackAsyncHandlers(event: ServerboundAttackPacketEvent) {
        if (attackAsyncHandlers.isEmpty())
            return
        
        for (handler in attackAsyncHandlers) {
            try {
                handler(event)
            } catch (e: Exception) {
                PacketEntityManager.logger.error("Exception in async PacketEntity attack handler", e)
            }
        }
    }
    
    fun runInteractHandlers(player: Player, hand: EquipmentSlot, interactLocation: Vector3dc): InteractionResult {
        if (interactHandlers.isEmpty())
            return InteractionResult.Pass
        
        val dsl = InteractDslImpl(player, hand, interactLocation)
        for (handler in interactHandlers) {
            val result = try {
                dsl.handler()
            } catch (e: Exception) {
                PacketEntityManager.logger.error("Exception in PacketEntity interact handler", e)
                InteractionResult.Pass
            }
            if (result !is InteractionResult.Pass)
                return result
        }
        
        return InteractionResult.Pass
    }
    
    fun runInteractAsyncHandlers(event: ServerboundInteractPacketEvent) {
        if (interactAsyncHandlers.isEmpty())
            return
        
        for (handler in interactAsyncHandlers) {
            try {
                handler(event)
            } catch (e: Exception) {
                PacketEntityManager.logger.error("Exception in async PacketEntity interact handler", e)
            }
        }
    }
    
}

internal class PacketEntityPassengerData<M : EntityMetadata>(
    private val type: EntityType<*>,
    private val state: PacketEntityState<*>,
    private val metadata: M
) {
    
    fun createNode(world: World): PacketEntityNodeImpl<M> =
        PacketEntityNodeImpl(type, state, metadata, world)
    
}

internal class PacketEntityImpl<M : EntityMetadata>(
    type: EntityType<*>,
    private val rootState: PacketEntityRootState<*>,
    metadata: M
) : PacketEntityNodeImpl<M>(type, rootState, metadata, rootState.location.get().world), PacketEntity<M> {
    
    override val visibility: PacketEntityVisibility = rootState.visibility
    override val spawnHandlers: MutableList<(Player) -> Unit> = rootState.spawnHandlers
    override val despawnHandlers: MutableList<(Player) -> Unit> = rootState.despawnHandlers
    override val attackHandlers: MutableList<AttackDsl.() -> Unit> = rootState.attackHandlers
    override val attackAsyncHandlers: MutableList<(ServerboundAttackPacketEvent) -> Unit> = rootState.attackAsyncHandlers
    override val interactHandlers: MutableList<InteractDsl.() -> InteractionResult> = rootState.interactHandlers
    override val interactAsyncHandlers: MutableList<(ServerboundInteractPacketEvent) -> Unit> = rootState.interactAsyncHandlers
    override var location by rootState.location
    override var viewerWhitelist by rootState.viewerWhitelist
    override var viewerBlacklist by rootState.viewerBlacklist
    private val sendMovementPackets = rootState.sendMovementPackets
    private val shouldBeSpawnedState = AtomicBoolean(false)
    private val managerUpdateQueued = AtomicBoolean(false)
    
    var actualLocation: Location = rootState.location.get().clone()
    var actualViewerWhitelist: Set<UUID>? = rootState.viewerWhitelist.get()?.toSet()
    var actualViewerBlacklist: Set<UUID> = rootState.viewerBlacklist.get().toSet()
    private val manager: PacketEntityManager
    private var rootObserved = false
    val graphRemovePacket: ClientboundRemoveEntitiesPacket =
        ClientboundRemoveEntitiesPacket(*graphEntities.mapToIntArray(PacketEntityNodeImpl<*>::id))
    
    val spawnBundlePacket: ClientboundBundlePacket
        get() = ClientboundBundlePacket(graphSpawnPackets)
    
    val graphSpawnPackets: List<Packet<in ClientGamePacketListener>>
        get() {
            return buildList {
                graphEntities.forEach { addAll(it.spawnPackets(actualLocation)) }
                graphEntities.forEach { add(it.passengersPacket) }
            }
        }
    
    init {
        val initialLocation = rootState.location.get()
        manager = PacketEntityManager[initialLocation.world]
    }
    
    val shouldBeSpawned: Boolean
        get() = shouldBeSpawnedState.get()
    var isSpawned = false
    
    fun queueFlush() {
        queueUpdate()
    }
    
    fun markUpdateDequeued() {
        managerUpdateQueued.set(false)
    }
    
    private fun queueUpdate() {
        if (managerUpdateQueued.compareAndSet(false, true))
            manager.queueUpdate(this)
    }
    
    fun runSpawnHandlers(player: Player) {
        if (spawnHandlers.isEmpty())
            return
        
        PacketEntityManager.queueMainThreadTask {
            spawnHandlers.forEach { it(player) }
        }
    }
    
    fun runDespawnHandlers(player: Player) {
        if (despawnHandlers.isEmpty())
            return
        
        PacketEntityManager.queueMainThreadTask {
            despawnHandlers.forEach { it(player) }
        }
    }
    
    override fun spawn() {
        if (shouldBeSpawnedState.compareAndSet(false, true))
            queueUpdate()
    }
    
    override fun despawn() {
        if (shouldBeSpawnedState.compareAndSet(true, false))
            queueUpdate()
    }
    
    override fun teleport(modifyLocation: Location.() -> Unit) {
        rootState.location.set(rootState.location.get().clone().apply(modifyLocation))
    }
    
    override fun observe(root: PacketEntityImpl<*>) {
        if (rootObserved)
            return
        
        rootObserved = true
        rootState.observeRoot(
            locationObserver = { if (markDirty(DIRTY_LOCATION)) queueFlush() },
            viewersObserver = { if (markDirty(DIRTY_VIEWERS)) queueFlush() }
        )
        super.observe(root)
    }
    
    override fun unobserve() {
        if (!rootObserved)
            return
        
        rootObserved = false
        rootState.unobserveRoot()
        super.unobserve()
    }
    
    fun flush(): List<Packet<*>> {
        return buildList {
            val dirtyFlags = dirtyFlags()
            
            if ((dirtyFlags and DIRTY_VIEWERS) != 0) {
                actualViewerWhitelist = rootState.viewerWhitelist.get()?.toSet()
                actualViewerBlacklist = rootState.viewerBlacklist.get().toSet()
            }
            
            if ((dirtyFlags and DIRTY_LOCATION) != 0) {
                val packet = flushLocation()
                if (packet != null)
                    add(packet)
            }
            
            addAll(flushNode(DIRTY_LOCATION or DIRTY_VIEWERS))
            graphEntities.forEach { entity ->
                if (entity !== this@PacketEntityImpl)
                    addAll(entity.flushNode())
            }
        }
    }
    
    fun applyChangesWithoutPackets() {
        flush()
        actualLocation = rootState.location.get().clone()
        actualViewerWhitelist = rootState.viewerWhitelist.get()?.toSet()
        actualViewerBlacklist = rootState.viewerBlacklist.get().toSet()
    }
    
    private fun flushLocation(): Packet<ClientGamePacketListener>? {
        val newLocation = rootState.location.get()
        require(newLocation.world == actualLocation.world) { "PacketEntity cannot move between worlds" }
        if (!sendMovementPackets) {
            actualLocation = newLocation.clone()
            return null
        }
        
        if (actualLocation.positionEquals(newLocation)) {
            if (newLocation.yaw != actualLocation.yaw || newLocation.pitch != actualLocation.pitch) {
                actualLocation = newLocation.clone()
                return ClientboundMoveEntityPacket.Rot(
                    id,
                    newLocation.yaw.toPackedByte(),
                    newLocation.pitch.toPackedByte(),
                    true
                )
            }
        } else if (actualLocation.distance(newLocation) > 8) {
            actualLocation = newLocation.clone()
            return ClientboundTeleportEntityPacket(
                id,
                PositionMoveRotation(
                    Vec3(newLocation.x(), newLocation.y(), newLocation.z()),
                    Vec3.ZERO,
                    newLocation.yaw,
                    newLocation.pitch
                ),
                emptySet(),
                true
            )
        } else {
            val deltaX = (newLocation.x - actualLocation.x).toFixedPoint()
            val deltaY = (newLocation.y - actualLocation.y).toFixedPoint()
            val deltaZ = (newLocation.z - actualLocation.z).toFixedPoint()
            
            actualLocation.add(deltaX.fromFixedPoint(), deltaY.fromFixedPoint(), deltaZ.fromFixedPoint())
            
            if (newLocation.yaw != actualLocation.yaw || newLocation.pitch != actualLocation.pitch) {
                actualLocation.yaw = newLocation.yaw
                actualLocation.pitch = newLocation.pitch
                
                return ClientboundMoveEntityPacket.PosRot(
                    id,
                    deltaX, deltaY, deltaZ,
                    newLocation.yaw.toPackedByte(),
                    newLocation.pitch.toPackedByte(),
                    true
                )
            } else {
                return ClientboundMoveEntityPacket.Pos(
                    id,
                    deltaX, deltaY, deltaZ,
                    true
                )
            }
        }
        
        return null
    }
    
}
