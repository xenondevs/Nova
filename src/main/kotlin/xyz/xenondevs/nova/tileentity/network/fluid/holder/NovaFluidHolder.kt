package xyz.xenondevs.nova.tileentity.network.fluid.holder

import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.associateWithToEnumMap
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

private val DEFAULT_CONNECTION_CONFIG = { CUBE_FACES.associateWithToEnumMap { NetworkConnectionType.NONE } }
private val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithToEnumMap { 0 } }
private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithToEnumMap { 50 } }

fun NovaFluidHolder(
    endPoint: NetworkedTileEntity,
    defaultContainer: Pair<FluidContainer, NetworkConnectionType>,
    vararg otherContainers: Pair<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer> = { CUBE_FACES.associateWithToEnumMap { defaultContainer.first } },
    defaultConnectionConfig: (() -> MutableMap<BlockFace, NetworkConnectionType>)? = null
): NovaFluidHolder {
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

class NovaFluidHolder(
    override val endPoint: NetworkedTileEntity,
    val availableContainers: Map<UUID, FluidContainer>,
    override val allowedConnectionTypes: Map<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer>,
    defaultConnectionConfig: (() -> MutableMap<BlockFace, NetworkConnectionType>)?
) : FluidHolder {
    
    override val containerConfig: MutableMap<BlockFace, FluidContainer> =
        (endPoint.retrieveEnumMapOrNull<BlockFace, UUID>("fluidContainerConfig")
            ?.mapValuesTo(emptyEnumMap()) { availableContainers[it.value] })
            ?: defaultContainerConfig()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        endPoint.retrieveDoubleEnumMap("fluidConnectionConfig", defaultConnectionConfig ?: DEFAULT_CONNECTION_CONFIG)
    
    override val channels: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("fluidChannels", DEFAULT_CHANNEL_CONFIG)
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("fluidInsertPriorities", DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        endPoint.retrieveEnumMap("fluidExtractPriorities", DEFAULT_PRIORITIES)
    
    override fun saveData() {
        endPoint.storeEnumMap("channels", channels)
        endPoint.storeEnumMap("fluidConnectionConfig", connectionConfig)
        endPoint.storeEnumMap("fluidInsertPriorities", insertPriorities)
        endPoint.storeEnumMap("fluidExtractPriorities", extractPriorities)
        
        if (availableContainers.isNotEmpty()) {
            endPoint.storeEnumMap("fluidContainerConfig", containerConfig.mapValues { it.value.uuid })
        }
    }
    
    companion object {
        
        fun modifyItemBuilder(builder: ItemBuilder, tileEntity: TileEntity?): ItemBuilder {
            if (tileEntity is NetworkedTileEntity) {
                val fluidHolder = tileEntity.fluidHolder as NovaFluidHolder
                fluidHolder.availableContainers.values.forEach { container ->
                    if (container.hasFluid()) builder.addLoreLines(localized(
                        ChatColor.GRAY,
                        "tooltip.nova.fluid",
                        TranslatableComponent(container.type!!.localizedName),
                        container.amount,
                        container.capacity
                    ))
                }
            }
            
            return builder
        }
        
    }
    
}