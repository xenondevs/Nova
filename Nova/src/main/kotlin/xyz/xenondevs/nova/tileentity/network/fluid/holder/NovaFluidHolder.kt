package xyz.xenondevs.nova.tileentity.network.fluid.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.container.NovaFluidContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.associateWithToEnumMap
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

private val DEFAULT_CONNECTION_CONFIG = { CUBE_FACES.associateWithToEnumMap { NetworkConnectionType.NONE } }
private val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithToEnumMap { 0 } }
private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithToEnumMap { 50 } }

fun <T> NovaFluidHolder(
    endPoint: T,
    defaultContainer: Pair<FluidContainer, NetworkConnectionType>,
    vararg otherContainers: Pair<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer> = { CUBE_FACES.associateWithToEnumMap { defaultContainer.first } },
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
    availableContainers: Map<UUID, FluidContainer>,
    allowedConnectionTypes: Map<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer>,
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
    val availableContainers: Map<UUID, FluidContainer>,
    override val allowedConnectionTypes: Map<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer>,
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)?
) : FluidHolder {
    
    override val containerConfig: MutableMap<BlockFace, FluidContainer> =
        (dataHolder.retrieveDataOrNull<EnumMap<BlockFace, UUID>>("fluidContainerConfig")
            ?.mapValuesTo(emptyEnumMap()) { availableContainers[it.value] })
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
        availableContainers.forEach { (_, container) -> if (container is NovaFluidContainer) container.reload() }
    }
    
    override fun saveData() {
        dataHolder.storeData("fluidChannels", channels)
        dataHolder.storeData("fluidConnectionConfig", connectionConfig)
        dataHolder.storeData("fluidInsertPriorities", insertPriorities)
        dataHolder.storeData("fluidExtractPriorities", extractPriorities)
        
        if (availableContainers.isNotEmpty()) {
            dataHolder.storeData("fluidContainerConfig", containerConfig.mapValuesTo(emptyEnumMap()) { it.value.uuid })
        }
    }
    
}