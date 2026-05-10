package xyz.xenondevs.nova.world.block.tileentity.network.node

import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.entry
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import java.util.*

abstract class DefaultContainerEndPointDataHolder<C : EndPointContainer> internal constructor(
    compound: Provider<Compound>,
    final override val containers: Map<C, NetworkConnectionType>,
    final override val blockedFaces: CubeFaceSet,
    defaultContainerConfig: CubeFaceMap<C?>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType>?
) : ContainerEndPointDataHolder<C> {
    
    init {
        if (containers.isEmpty())
            throw IllegalArgumentException("availableContainers must not be empty")
    }
    
    protected abstract val uuidToContainer: Map<UUID, C>
    
    final override var containerConfig: CubeFaceMap<C?>
        by compound.entry<CubeFaceMap<UUID?>>("containerConfig")
            .mapNonNull(
                { it.map { uuid -> uuid?.let(uuidToContainer::get) } },
                { it.map { container -> container?.uuid } }
            ).orElse(defaultContainerConfig)
    
    final override var connectionConfig: CubeFaceMap<NetworkConnectionType>
        by compound.entry<CubeFaceMap<NetworkConnectionType>>("connectionConfig")
            .orElse(
                (defaultConnectionConfig ?: containerConfig.map { containers[it]!! })
                    .map { f, t -> if (f !in blockedFaces) t else NetworkConnectionType.NONE }
            )
    
    final override var channels: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("channels")
            .orElse(DEFAULT_CHANNEL_CONFIG)
    
    final override var insertPriorities: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("insertPriorities")
            .orElse(DEFAULT_PRIORITIES)
    
    final override var extractPriorities: CubeFaceMap<Int>
        by compound.entry<CubeFaceMap<Int>>("extractPriorities")
            .orElse(DEFAULT_PRIORITIES)
    
    internal companion object {
        
        val DEFAULT_CHANNEL_CONFIG = CubeFaceMap(0)
        val DEFAULT_PRIORITIES = CubeFaceMap(50)
        
    }
    
}