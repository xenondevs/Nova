@file:Suppress("LeakingThis")

package xyz.xenondevs.nova.world.block.tileentity

import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.util.BlockSideMap
import xyz.xenondevs.nova.util.BlockSideSet
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.DefaultFluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.DefaultItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedMultiVirtualInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedVirtualInventory
import java.util.*

abstract class NetworkedTileEntity(
    pos: BlockPos,
    blockState: NovaBlockState,
    data: Compound
) : TileEntity(pos, blockState, data), NetworkEndPoint {
    
    @Volatile
    final override var isValid = false
    
    final override val holders: MutableCollection<EndPointDataHolder> = ArrayList()
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    /**
     * Retrieves the [EnergyHolder] previously stored or creates a new one and registers it in the [holders] map.
     *
     * The energy capacity is limited by the [maxEnergy] provider and the [allowedConnectionType] determines
     * whether energy can be inserted, extracted, or both.
     *
     * The [blockedSides] defines which sides of this tile-entity can never be used for energy transfer.
     *
     * If the [EnergyHolder] is created for the first time, [defaultConnectionConfig] is used to determine the
     * correct [NetworkConnectionType] for each side.
     */
    @JvmName("storedEnergyHolderBlockSide")
    fun storedEnergyHolder(
        maxEnergy: Provider<Long>,
        allowedConnectionType: NetworkConnectionType,
        blockedSides: BlockSideSet,
        defaultConnectionConfig: BlockSideMap<NetworkConnectionType> = BlockSideMap(allowedConnectionType)
    ): DefaultEnergyHolder {
        val front = blockState[DefaultBlockStateProperties.FACING] ?: BlockFace.NORTH
        return storedEnergyHolder(
            maxEnergy,
            allowedConnectionType,
            blockedSides.toCubeFaceSet(front),
            defaultConnectionConfig.toCubeFaceMap(front)
        )
    }
    
    /**
     * Retrieves the [EnergyHolder] previously stored or creates a new one and registers it in the [holders] map.
     *
     * The energy capacity is limited by the [maxEnergy] provider and the [allowedConnectionType] determines
     * whether energy can be inserted, extracted, or both.
     *
     * The [blockedFaces] define which faces of this tile-entity can never be used for energy transfer.
     *
     * If the [EnergyHolder] is created for the first time, [defaultConnectionConfig] is used to determine the
     * correct [NetworkConnectionType] for each side.
     */
    @JvmName("storedEnergyHolderBlockFace")
    fun storedEnergyHolder(
        maxEnergy: Provider<Long>,
        allowedConnectionType: NetworkConnectionType,
        blockedFaces: CubeFaceSet = CubeFaceSet.NONE,
        defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = CubeFaceMap(allowedConnectionType)
    ): DefaultEnergyHolder {
        val holder = DefaultEnergyHolder(
            storedValue("energyHolder", ::Compound),
            storedValue("energy", true) { 0L },
            maxEnergy,
            allowedConnectionType,
            blockedFaces,
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
     * The [blockedSides] define which sides of the tile-entity can never be used for item transfer.
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [VirtualInventory] and [NetworkConnectionType] for each side.
     * If [defaultInventoryConfig] is `null`, the merged inventory will be used for all sides.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedItemHolderBlockSide")
    fun storedItemHolder(
        inventory: Pair<VirtualInventory, NetworkConnectionType>,
        vararg inventories: Pair<VirtualInventory, NetworkConnectionType>,
        blockedSides: BlockSideSet,
        defaultInventoryConfig: BlockSideMap<VirtualInventory?>? = null,
        defaultConnectionConfig: BlockSideMap<NetworkConnectionType>? = null,
    ): DefaultItemHolder {
        val front = blockState[DefaultBlockStateProperties.FACING] ?: BlockFace.NORTH
        return storedItemHolder(
            inventory,
            inventories = inventories,
            blockedSides.toCubeFaceSet(front),
            defaultInventoryConfig?.toCubeFaceMap(front),
            defaultConnectionConfig?.toCubeFaceMap(front)
        )
    }
    
    /**
     * Retrieves the [ItemHolder] previously stored or creates a new one, registers it in the [holders] map,
     * and adds drop providers for [ItemHolder.insertFilters] and [ItemHolder.extractFilters].
     *
     * The item holder uses the inventories and connection types provided ([inventory], [inventories]).
     *
     * The [blockedFaces] define which faces of the tile-entity can never be used for item transfer.
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [VirtualInventory] and [NetworkConnectionType] for each side.
     * If [defaultInventoryConfig] is `null`, the merged inventory will be used for all sides.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedItemHolderBlockFace")
    fun storedItemHolder(
        inventory: Pair<VirtualInventory, NetworkConnectionType>,
        vararg inventories: Pair<VirtualInventory, NetworkConnectionType>,
        blockedFaces: CubeFaceSet = CubeFaceSet.NONE,
        defaultInventoryConfig: CubeFaceMap<VirtualInventory?>? = null,
        defaultConnectionConfig: CubeFaceMap<NetworkConnectionType>? = null,
    ): DefaultItemHolder {
        val allInventories: Map<VirtualInventory, NetworkConnectionType> =
            buildMap { this += inventory; this += inventories }
        val availableInventories: MutableMap<UUID, NetworkedInventory> =
            allInventories.keys.associateTo(HashMap()) { it.uuid to NetworkedVirtualInventory(it) }
        val allowedConnectionTypes: MutableMap<NetworkedInventory, NetworkConnectionType> =
            allInventories.mapKeysTo(HashMap()) { [vi, _] -> availableInventories[vi.uuid]!! }
        
        val mergedInventory = NetworkedMultiVirtualInventory(DefaultItemHolder.ALL_INVENTORY_UUID, allInventories)
        availableInventories[DefaultItemHolder.ALL_INVENTORY_UUID] = mergedInventory
        allowedConnectionTypes[mergedInventory] = NetworkConnectionType.of(allowedConnectionTypes.values)
        
        val holder = DefaultItemHolder(
            storedValue("itemHolder", ::Compound),
            allowedConnectionTypes,
            mergedInventory,
            blockedFaces,
            // map from VirtualInventory to NetworkedInventory or use mergedInventory for all sides
            defaultInventoryConfig
                ?.map { it?.let { availableInventories[it.uuid] } }
                ?: CubeFaceMap(mergedInventory),
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
     * The [blockedSides] define which sides of the tile-entity can never be used for item transfer.
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedInventory] and [NetworkConnectionType] for each side.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedItemHolderBlockSide")
    fun storedItemHolder(
        inventory: Pair<NetworkedInventory, NetworkConnectionType>,
        vararg inventories: Pair<NetworkedInventory, NetworkConnectionType>,
        mergedInventory: NetworkedInventory? = null,
        blockedSides: BlockSideSet,
        defaultInventoryConfig: BlockSideMap<NetworkedInventory?> = BlockSideMap(inventory.first),
        defaultConnectionConfig: BlockSideMap<NetworkConnectionType>? = null
    ): DefaultItemHolder {
        val front = blockState[DefaultBlockStateProperties.FACING] ?: BlockFace.NORTH
        return storedItemHolder(
            inventory,
            inventories = inventories,
            mergedInventory,
            blockedSides.toCubeFaceSet(front),
            defaultInventoryConfig.toCubeFaceMap(front),
            defaultConnectionConfig?.toCubeFaceMap(front)
        )
    }
    
    /**
     * Retrieves the [ItemHolder] previously stored or creates a new one, registers it in the [holders] map,
     * and adds drop providers for [ItemHolder.insertFilters] and [ItemHolder.extractFilters].
     *
     * The item holder uses the inventories and connection types provided ([inventory], [inventories]).
     *
     * The [blockedFaces] define which faces of the tile-entity can never be used for item transfer.
     *
     * If the [ItemHolder] is created for the first time, [defaultInventoryConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedInventory] and [NetworkConnectionType] for each side.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedItemHolderBlockFace")
    fun storedItemHolder(
        inventory: Pair<NetworkedInventory, NetworkConnectionType>,
        vararg inventories: Pair<NetworkedInventory, NetworkConnectionType>,
        mergedInventory: NetworkedInventory? = null,
        blockedFaces: CubeFaceSet = CubeFaceSet.NONE,
        defaultInventoryConfig: CubeFaceMap<NetworkedInventory?> = CubeFaceMap(inventory.first),
        defaultConnectionConfig: CubeFaceMap<NetworkConnectionType>? = null,
    ): DefaultItemHolder {
        val allInventories = buildMap { this += inventory; this += inventories }
        
        val holder = DefaultItemHolder(
            storedValue("itemHolder", ::Compound),
            allInventories,
            mergedInventory,
            blockedFaces,
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
            holder.insertFilters.forEach { if (it != null) itemFilters += it.toItemStack() }
            holder.extractFilters.forEach { if (it != null) itemFilters += it.toItemStack() }
            itemFilters
        }
    }
    
    /**
     * Retrieves the [FluidHolder] previously stored or creates a new one and registers it in the [holders] map.
     *
     * The fluid holder uses the containers and connection types provided ([container], [containers]).
     *
     * The [blockedSides] define which sides of the tile-entity can never be used for fluid transfer.
     *
     * If the [FluidHolder] is created for the first time, [defaultContainerConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedFluidContainer] and [NetworkConnectionType] for each side.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedFluidHolderBlockSide")
    fun storedFluidHolder(
        container: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        vararg containers: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        blockedSides: BlockSideSet,
        defaultContainerConfig: BlockSideMap<NetworkedFluidContainer?> = BlockSideMap(container.first),
        defaultConnectionConfig: BlockSideMap<NetworkConnectionType>? = null
    ): DefaultFluidHolder {
        val front = blockState[DefaultBlockStateProperties.FACING] ?: BlockFace.NORTH
        return storedFluidHolder(
            container,
            containers = containers,
            blockedSides.toCubeFaceSet(front),
            defaultContainerConfig.toCubeFaceMap(front),
            defaultConnectionConfig?.toCubeFaceMap(front)
        )
    }
    
    
    /**
     * Retrieves the [FluidHolder] previously stored or creates a new one and registers it in the [holders] map.
     *
     * The fluid holder uses the containers and connection types provided ([container], [containers]).
     *
     * The [blockedFaces] define which faces of the tile-entity can never be used for fluid transfer.
     *
     * If the [FluidHolder] is created for the first time, [defaultContainerConfig] and [defaultConnectionConfig]
     * are used to determine the correct [NetworkedFluidContainer] and [NetworkConnectionType] for each side.
     *
     * If [defaultConnectionConfig] is `null`, each side will be assigned the highest possible connection type.
     */
    @JvmName("storedFluidHolderBlockFace")
    fun storedFluidHolder(
        container: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        vararg containers: Pair<NetworkedFluidContainer, NetworkConnectionType>,
        blockedFaces: CubeFaceSet = CubeFaceSet.NONE,
        defaultContainerConfig: CubeFaceMap<NetworkedFluidContainer?> = CubeFaceMap(container.first),
        defaultConnectionConfig: CubeFaceMap<NetworkConnectionType>? = null
    ): DefaultFluidHolder {
        val fluidHolder = DefaultFluidHolder(
            storedValue("fluidHolder", ::Compound),
            buildMap { this += container; this += containers },
            blockedFaces,
            defaultContainerConfig,
            defaultConnectionConfig
        )
        holders += fluidHolder
        return fluidHolder
    }
    
    override fun handleEnable() {
        super.handleEnable()
        isValid = true
    }
    
    override fun handlePlace(ctx: Context<BlockPlace>) {
        super.handlePlace(ctx)
        NetworkManager.queueAddEndPoint(this)
        isValid = true
    }
    
    override fun handleDisable() {
        super.handleDisable()
        isValid = false
    }
    
    override fun handleBreak(ctx: Context<BlockBreak>) {
        super.handleBreak(ctx)
        NetworkManager.queueRemoveEndPoint(this)
        isValid = false
    }
    
}