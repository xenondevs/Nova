package xyz.xenondevs.nova.tileentity.network.fluid.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import java.util.*

private val DEFAULT_CONNECTION_CONFIG = { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.NONE } }
private val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithTo(enumMap()) { 0 } }
private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithTo(enumMap()) { 50 } }

fun <T> NovaFluidHolder(
    endPoint: T,
    defaultContainer: Pair<NetworkedFluidContainer, NetworkConnectionType>,
    vararg otherContainers: Pair<NetworkedFluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, NetworkedFluidContainer> = { CUBE_FACES.associateWithTo(enumMap()) { defaultContainer.first } },
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)? = null
): NovaFluidHolder where T : NetworkEndPoint, T : DataHolder {
    val containers = hashMapOf(defaultContainer).also { it.putAll(otherContainers) }
    val availableContainers = containers.keys.associateByTo(HashMap()) { it.uuid }
    
    return NovaFluidHolder(
        endPoint,
        availableContainers,
        containers,
        defaultContainerConfig,
        defaultConnectionConfig
    )
}

fun <T> NovaFluidHolder(
    endPoint: T,
    availableContainers: Map<UUID, NetworkedFluidContainer>,
    allowedConnectionTypes: Map<NetworkedFluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, NetworkedFluidContainer>,
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)?
): NovaFluidHolder where T : NetworkEndPoint, T : DataHolder =
    NovaFluidHolder(
        endPoint,
        endPoint,
        availableContainers,
        allowedConnectionTypes,
        defaultContainerConfig,
        defaultConnectionConfig
    )

class NovaFluidHolder(
    override val endPoint: NetworkEndPoint,
    private val dataHolder: DataHolder,
    val availableContainers: Map<UUID, NetworkedFluidContainer>,
    override val allowedConnectionTypes: Map<NetworkedFluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, NetworkedFluidContainer>,
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)?
) : FluidHolder {
    
    override val containerConfig: MutableMap<BlockFace, NetworkedFluidContainer> =
        (dataHolder.retrieveDataOrNull<EnumMap<BlockFace, UUID>>("fluidContainerConfig")
            ?.mapValuesTo(enumMap()) { availableContainers[it.value] })
            ?: defaultContainerConfig()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        dataHolder.retrieveData("fluidConnectionConfig", defaultConnectionConfig ?: DEFAULT_CONNECTION_CONFIG)
    
    override val channels: MutableMap<BlockFace, Int> =
        dataHolder.retrieveData("fluidChannels", DEFAULT_CHANNEL_CONFIG)
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        dataHolder.retrieveData("fluidInsertPriorities", DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        dataHolder.retrieveData("fluidExtractPriorities", DEFAULT_PRIORITIES)
    
    override fun reload() {
        // TODO
//        availableContainers.forEach { (_, container) -> if (container is NovaFluidContainer) container.reload() }
    }
    
    override fun saveData() {
        dataHolder.storeData("fluidChannels", channels)
        dataHolder.storeData("fluidConnectionConfig", connectionConfig)
        dataHolder.storeData("fluidInsertPriorities", insertPriorities)
        dataHolder.storeData("fluidExtractPriorities", extractPriorities)
        
        if (availableContainers.isNotEmpty()) {
            dataHolder.storeData("fluidContainerConfig", containerConfig.mapValuesTo(enumMap()) { it.value.uuid })
        }
    }
    
}