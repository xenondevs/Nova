package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.collections.mapValuesNotNullTo
import xyz.xenondevs.commons.collections.toEnumMap
import xyz.xenondevs.commons.collections.toEnumSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.observed
import xyz.xenondevs.commons.provider.orElseNew
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import java.util.*

abstract class DefaultContainerEndPointDataHolder<C : EndPointContainer> internal constructor(
    compound: Provider<Compound>,
    final override val containers: Map<C, NetworkConnectionType>,
    blockedFaces: Set<BlockFace>,
    defaultContainerConfig: () -> Map<BlockFace, C>,
    defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)?
) : ContainerEndPointDataHolder<C> {
    
    init {
        if (containers.isEmpty())
            throw IllegalArgumentException("availableContainers must not be empty")
    }
    
    protected abstract val uuidToContainer: Map<UUID, C>
    
    final override val blockedFaces = blockedFaces.toEnumSet()
    
    final override val containerConfig: MutableMap<BlockFace, C>
        by compound.entry<Map<BlockFace, UUID>>("containerConfig")
            .mapNonNull(
                { it.mapValuesNotNullTo(enumMap()) { (_, uuid) -> uuidToContainer[uuid] } },
                { it.mapValuesTo(enumMap()) { (_, container) -> container.uuid } }
            ).orElseNew { defaultContainerConfig().toEnumMap() }
            .observed()
    
    final override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
        by compound.entry<MutableMap<BlockFace, NetworkConnectionType>>("connectionConfig")
            .orElseNew {
                val map: MutableMap<BlockFace, NetworkConnectionType> = defaultConnectionConfig?.invoke()?.toEnumMap()
                    ?: containerConfig.mapValuesTo(enumMap()) { (_, container) -> containers[container] }
                for (face in blockedFaces)
                    map[face] = NetworkConnectionType.NONE
                map
            }
            .observed()
    
    final override val channels: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("channels")
            .orElseNew(DEFAULT_CHANNEL_CONFIG)
            .observed()
    
    final override val insertPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("insertPriorities")
            .orElseNew(DEFAULT_PRIORITIES)
            .observed()
    
    final override val extractPriorities: MutableMap<BlockFace, Int>
        by compound.entry<MutableMap<BlockFace, Int>>("extractPriorities")
            .orElseNew(DEFAULT_PRIORITIES)
            .observed()
    
    internal companion object {
        
        val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
        val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }
        
    }
    
}