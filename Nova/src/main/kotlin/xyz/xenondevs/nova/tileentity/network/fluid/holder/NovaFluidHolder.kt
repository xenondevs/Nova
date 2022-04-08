package xyz.xenondevs.nova.tileentity.network.fluid.holder

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.associateWithToEnumMap
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

private val DEFAULT_CONNECTION_CONFIG = { CUBE_FACES.associateWithToEnumMap { NetworkConnectionType.NONE } }
private val DEFAULT_CHANNEL_CONFIG = { CUBE_FACES.associateWithToEnumMap { 0 } }
private val DEFAULT_PRIORITIES = { CUBE_FACES.associateWithToEnumMap { 50 } }

fun <T> FluidHolder(
    endPoint: T,
    defaultContainer: Pair<FluidContainer, NetworkConnectionType>,
    vararg otherContainers: Pair<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer> = { CUBE_FACES.associateWithToEnumMap { defaultContainer.first } },
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)? = null
): FluidHolder where T : NetworkEndPoint, T : DataHolder {
    val containers = hashMapOf(defaultContainer).also { it.putAll(otherContainers) }
    val availableContainers = containers.keys.associateByTo(HashMap()) { it.uuid }
    
    return FluidHolder(
        endPoint,
        availableContainers,
        containers,
        defaultContainerConfig,
        defaultConnectionConfig
    )
}

fun <T> FluidHolder(
    endPoint: T,
    availableContainers: Map<UUID, FluidContainer>,
    allowedConnectionTypes: Map<FluidContainer, NetworkConnectionType>,
    defaultContainerConfig: () -> MutableMap<BlockFace, FluidContainer>,
    defaultConnectionConfig: (() -> EnumMap<BlockFace, NetworkConnectionType>)?
): FluidHolder where T : NetworkEndPoint, T : DataHolder =
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
        (dataHolder.retrieveEnumMapOrNull<BlockFace, UUID>("fluidContainerConfig")
            ?.mapValuesTo(emptyEnumMap()) { availableContainers[it.value] })
            ?: defaultContainerConfig()
    
    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> =
        dataHolder.retrieveEnumMap("fluidConnectionConfig", defaultConnectionConfig ?: DEFAULT_CONNECTION_CONFIG)
    
    override val channels: MutableMap<BlockFace, Int> =
        dataHolder.retrieveEnumMap("fluidChannels", DEFAULT_CHANNEL_CONFIG)
    
    override val insertPriorities: MutableMap<BlockFace, Int> =
        dataHolder.retrieveEnumMap("fluidInsertPriorities", DEFAULT_PRIORITIES)
    
    override val extractPriorities: MutableMap<BlockFace, Int> =
        dataHolder.retrieveEnumMap("fluidExtractPriorities", DEFAULT_PRIORITIES)
    
    override fun saveData() {
        dataHolder.storeEnumMap("channels", channels)
        dataHolder.storeEnumMap("fluidConnectionConfig", connectionConfig)
        dataHolder.storeEnumMap("fluidInsertPriorities", insertPriorities)
        dataHolder.storeEnumMap("fluidExtractPriorities", extractPriorities)
        
        if (availableContainers.isNotEmpty()) {
            dataHolder.storeEnumMap("fluidContainerConfig", containerConfig.mapValues { it.value.uuid })
        }
    }
    
    companion object {
        
        fun modifyItemBuilder(builder: ItemBuilder, tileEntity: TileEntity?): ItemBuilder {
            if (tileEntity is NetworkedTileEntity) {
                val fluidHolder = tileEntity.fluidHolder as NovaFluidHolder
                fluidHolder.availableContainers.values.forEach { container ->
                    if (container.hasFluid()) {
                        val amount = container.amount
                        val capacity = container.capacity
                        
                        val amountStr = if (amount != Long.MAX_VALUE) {
                            if (capacity == Long.MAX_VALUE) NumberFormatUtils.getFluidString(amount) + " / ∞ mB"
                            else NumberFormatUtils.getFluidString(amount, capacity)
                        } else "∞ mB / ∞ mB"
                        
                        builder.addLoreLines(
                            ComponentBuilder()
                                .color(ChatColor.GRAY)
                                .append(TranslatableComponent(container.type!!.localizedName))
                                .append(": $amountStr")
                                .create()
                        )
                    }
                }
            }
            
            return builder
        }
        
    }
    
}