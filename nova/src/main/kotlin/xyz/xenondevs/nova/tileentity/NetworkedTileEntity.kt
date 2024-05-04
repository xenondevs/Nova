package xyz.xenondevs.nova.tileentity

import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.DefaultFluidHolder
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.DefaultItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedMultiVirtualInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import java.util.*

abstract class NetworkedTileEntity(
    pos: BlockPos,
    blockState: NovaBlockState,
    data: Compound
) : TileEntity(pos, blockState, data), NetworkEndPoint {
    
    final override val holders: MutableSet<EndPointDataHolder> = HashSet()
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    /**
     * Retrieves the [EnergyHolder] previously stored or creates a new one and registers it in the [holders] map.
     *
     * The energy capacity is limited by the [maxEnergy] provider and the [allowedConnectionType] determines
     * whether energy can be inserted, extracted, or both.
     *
     * If the [EnergyHolder] is created for the first time, [defaultConnectionConfig] is used to determine the
     * correct [NetworkConnectionType] for each side.
     */
    fun storedEnergyHolder(
        maxEnergy: Provider<Long>,
        allowedConnectionType: NetworkConnectionType,
        defaultConnectionConfig: () -> Map<BlockFace, NetworkConnectionType> = { CUBE_FACES.associateWithTo(enumMap()) { allowedConnectionType } }
    ): DefaultEnergyHolder {
        val holder = DefaultEnergyHolder(
            storedValue("energyHolder", ::Compound).get(), // TODO: legacy conversion
            maxEnergy,
            allowedConnectionType,
            defaultConnectionConfig
        )
        holders += holder
        return holder
    }
    
    /**
     * Retrieves the [ItemHolder] previously stored or creates a new one, registers it in the [holders] map,
     * and adds drop providers for [ItemHolder.insertFilters] and [ItemHolder.extractFilters].
     *
     * The item holder uses the inventories and connection types provided ([inventory], [inventories]).
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [VirtualInventory] and [NetworkConnectionType] for each side.
     * If [defaultInventoryConfig] is `null`, the merged inventory will be used for all sides.
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    fun storedItemHolder(
        inventory: Pair<VirtualInventory, NetworkConnectionType>,
        vararg inventories: Pair<VirtualInventory, NetworkConnectionType>,
        defaultInventoryConfig: (() -> Map<BlockFace, VirtualInventory>)? = null,
        defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)? = null,
    ): DefaultItemHolder {
        val allInventories: Map<VirtualInventory, NetworkConnectionType> =
            buildMap { this += inventory; this += inventories }
        val availableInventories: MutableMap<UUID, NetworkedInventory> =
            allInventories.keys.associateTo(HashMap()) { it.uuid to NetworkedVirtualInventory(it) }
        val allowedConnectionTypes: MutableMap<NetworkedInventory, NetworkConnectionType> =
            allInventories.mapKeysTo(HashMap()) { (vi, _) -> availableInventories[vi.uuid]!! }
        
        val mergedInventory = NetworkedMultiVirtualInventory(DefaultItemHolder.ALL_INVENTORY_UUID, allInventories)
        availableInventories[DefaultItemHolder.ALL_INVENTORY_UUID] = mergedInventory
        allowedConnectionTypes[mergedInventory] = NetworkConnectionType.of(allowedConnectionTypes.values)
        
        val holder = DefaultItemHolder(
            storedValue("itemHolder", ::Compound).get(), // TODO: legacy conversion
            allowedConnectionTypes,
            mergedInventory,
            // map from VirtualInventory to NetworkedInventory or use mergedInventory for all sides
            defaultInventoryConfig
                ?.let { { it.invoke().mapValues { (_, vi) -> availableInventories[vi.uuid]!! } } }
                ?: { CUBE_FACES.associateWithTo(enumMap()) { mergedInventory } },
            defaultConnectionConfig
        )
        registerItemHolder(holder)
        return holder
    }
    
    /**
     * Retrieves the [ItemHolder] previously stored or creates a new one, registers it in the [holders] map,
     * and adds drop providers for [ItemHolder.insertFilters] and [ItemHolder.extractFilters].
     *
     * The item holder uses the inventories and connection types provided ([inventory], [inventories]).
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedInventory] and [NetworkConnectionType] for each side.
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    fun storedItemHolder(
        inventory: Pair<NetworkedInventory, NetworkConnectionType>,
        vararg inventories: Pair<NetworkedInventory, NetworkConnectionType>,
        mergedInventory: NetworkedInventory? = null,
        defaultInventoryConfig: () -> Map<BlockFace, NetworkedInventory> = { CUBE_FACES.associateWithTo(enumMap()) { inventory.first } },
        defaultConnectionConfig: (() -> Map<BlockFace, NetworkConnectionType>)? = null
    ): DefaultItemHolder {
        val allInventories = buildMap { this += inventory; this += inventories }
        val holder = DefaultItemHolder(
            storedValue("itemHolder", ::Compound).get(), // TODO: legacy conversion
            allInventories,
            mergedInventory,
            defaultInventoryConfig,
            defaultConnectionConfig
        )
        registerItemHolder(holder)
        return holder
    }
    
    /**
     * Registers the given [holder] to [holders] and adds drop providers for [ItemHolder.insertFilters]
     * and [ItemHolder.extractFilters].
     */
    private fun registerItemHolder(holder: ItemHolder) {
        holders += holder
        dropProvider {
            val itemFilters = ArrayList<ItemStack>()
            for (filter in holder.insertFilters.values)
                itemFilters += filter.createFilterItem()
            for (filter in holder.extractFilters.values)
                itemFilters += filter.createFilterItem()
            itemFilters
        }
    }
    
    /**
     * Retrieves the [FluidHolder] previously stored or creates a new one and registers it in the [holders] map.
     * 
     * The fluid holder uses the containers and connection types provided ([container], [containers]).
     * 
     * If the [FluidHolder] is created for the first time, [defaultContainerConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedFluidContainer] and [NetworkConnectionType] for each side.
     */
    fun storedFluidHolder(
        container: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        vararg containers: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        defaultContainerConfig: () -> MutableMap<BlockFace, NetworkedFluidContainer> = { CUBE_FACES.associateWithTo(enumMap()) { container.first } },
        defaultConnectionConfig: () -> EnumMap<BlockFace, NetworkConnectionType> = DefaultFluidHolder.DEFAULT_CONNECTION_CONFIG
    ): DefaultFluidHolder {
        val fluidHolder = DefaultFluidHolder(
            storedValue("fluidHolder", ::Compound).get(), // TODO: legacy conversion
            buildMap { this += container; this += containers },
            defaultContainerConfig,
            defaultConnectionConfig
        )
        holders += fluidHolder
        return fluidHolder
    }
    
    override fun handlePlace(ctx: Context<ContextIntentions.BlockPlace>) {
        super.handlePlace(ctx)
        NetworkManager.queueAddEndPoint(this)
    }
    
    override fun handleBreak(ctx: Context<ContextIntentions.BlockBreak>) {
        super.handleBreak(ctx)
        NetworkManager.queueRemoveEndPoint(this)
    }
    
    override fun saveData() {
        super.saveData()
        holders.forEach(EndPointDataHolder::saveData)
    }
    
}