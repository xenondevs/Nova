package xyz.xenondevs.nova.packetentity

import org.bukkit.entity.Player
import org.joml.Vector3dc
import java.util.*

internal class InteractDslImpl(
    override val player: Player,
    override val interactLocation: Vector3dc
) : InteractDsl

internal class SpawnDslImpl(override val viewer: Player) : SpawnDsl

internal class DespawnDslImpl(override val viewer: Player) : DespawnDsl

internal open class PacketEntityState<M : EntityMetadataState>(val metadata: M) {
    
    val attributes = PacketEntityAttributes()
    val equipment = PacketEntityEquipmentState()
    val components: List<ReactiveDataValue<*>> = buildList(metadata::addComponents)
    val passengers = ArrayList<PacketEntityPassengerData<*>>()
    val interactHandlers = ArrayList<InteractDsl.() -> Unit>()
    
    fun observe(
        attributesObserver: () -> Unit,
        equipmentObserver: () -> Unit,
        metadataObserver: (Int) -> Unit
    ) {
        attributes.observe(attributesObserver)
        equipment.observe(equipmentObserver)
        for (i in components.indices) {
            components[i].value.observe { metadataObserver(i) }
        }
    }
    
    fun unobserve() {
        attributes.unobserve()
        equipment.unobserve()
        components.forEach { it.value.unobserve() }
    }
    
    fun buildFullAttributesPacket(entityId: Int) =
        attributes.buildFullPacket(entityId)
    
    fun buildDirtyAttributesPacket(entityId: Int) =
        attributes.buildDirtyPacket(entityId)
    
    fun buildEquipmentPacket(entityId: Int) =
        equipment.buildPacket(entityId)
    
}

internal class PacketEntityRootState<M : EntityMetadataState>(metadata: M) : PacketEntityState<M>(metadata) {
    
    var lod: PacketEntityLod = PacketEntityLod.ALL
    var sendMovementPackets: Boolean = true
    val location = DefaultEntityValue(org.bukkit.Location(null, 0.0, 0.0, 0.0))
    val viewerWhitelist = DefaultEntityValue<Set<UUID>?>(null)
    val viewerBlacklist = DefaultEntityValue<Set<UUID>>(emptySet())
    val spawnHandlers = ArrayList<(Player) -> Unit>()
    val despawnHandlers = ArrayList<(Player) -> Unit>()
    
    fun observeRoot(
        locationObserver: () -> Unit,
        viewersObserver: () -> Unit
    ) {
        location.observe(locationObserver)
        viewerWhitelist.observe(viewersObserver)
        viewerBlacklist.observe(viewersObserver)
    }
    
    fun unobserveRoot() {
        location.unobserve()
        viewerWhitelist.unobserve()
        viewerBlacklist.unobserve()
    }
    
}

internal abstract class PacketEntityDslBase<M : EntityMetadataDsl>(
    protected val packetState: PacketEntityState<*>,
    private val metadataDsl: M
) {
    
    open val attributes get() = AttributesDslPropertyImpl(packetState.attributes)
    open val equipment get() = EquipmentDslPropertyImpl(packetState.equipment)
    
    open fun equipment(equipment: EquipmentDsl.() -> Unit) {
        EquipmentDslImpl(packetState.equipment).equipment()
    }
    
    open fun metadata(metadata: M.() -> Unit) {
        metadataDsl.metadata()
    }
    
    open fun passengers(passengers: PacketEntityPassengersDsl.() -> Unit) {
        PacketEntityPassengersDslImpl(packetState).passengers()
    }
    
    open fun onInteract(handler: InteractDsl.() -> Unit) {
        packetState.interactHandlers += handler
    }
    
}

internal class PacketEntityDslImpl<M : EntityMetadataDsl>(
    private val rootState: PacketEntityRootState<*>,
    metadataDsl: M
) : PacketEntityDslBase<M>(rootState, metadataDsl), PacketEntityDsl<M> {
    
    override var lod by rootState::lod
    override var sendMovementPackets by rootState::sendMovementPackets
    override val location get() = rootState.location
    override val attributes get() = super.attributes
    override val equipment get() = super.equipment
    override val viewerWhitelist get() = rootState.viewerWhitelist
    override val viewerBlacklist get() = rootState.viewerBlacklist
    
    override fun onSpawn(handler: SpawnDsl.() -> Unit) {
        rootState.spawnHandlers += { player -> SpawnDslImpl(player).handler() }
    }
    
    override fun onDespawn(handler: DespawnDsl.() -> Unit) {
        rootState.despawnHandlers += { player -> DespawnDslImpl(player).handler() }
    }
    
}

internal class PassengerPacketEntityDslImpl<M : EntityMetadataDsl>(
    packetState: PacketEntityState<*>,
    metadataDsl: M
) : PacketEntityDslBase<M>(packetState, metadataDsl), PassengerPacketEntityDsl<M> {
    
    override val attributes get() = super.attributes
    override val equipment get() = super.equipment
    
    override fun equipment(equipment: EquipmentDsl.() -> Unit) =
        super.equipment(equipment)
    
    override fun metadata(metadata: M.() -> Unit) =
        super.metadata(metadata)
    
    override fun passengers(passengers: PacketEntityPassengersDsl.() -> Unit) =
        super.passengers(passengers)
    
    override fun onInteract(handler: InteractDsl.() -> Unit) =
        super.onInteract(handler)
    
}

internal class PacketEntityPassengersDslImpl(
    private val packetState: PacketEntityState<*>
) : PacketEntityPassengersDsl {
    
    fun add(passenger: PacketEntityPassengerData<*>) {
        packetState.passengers += passenger
    }
    
}
