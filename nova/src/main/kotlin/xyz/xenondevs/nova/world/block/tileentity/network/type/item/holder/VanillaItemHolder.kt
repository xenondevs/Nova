package xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder

import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.entity.BlockEntity
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_CHANNEL_CONFIG
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_PRIORITIES
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory

private val ALL_BUFFER = CubeFaceMap(NetworkConnectionType.BUFFER)

internal abstract class VanillaItemHolder(
    private val blockEntity: BlockEntity,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : ItemHolder {
    
    private val pdc = blockEntity.persistentDataContainer
    
    override val mergedInventory: NetworkedInventory? = null
    
    override var connectionConfig: CubeFaceMap<NetworkConnectionType> =
        pdc[CONNECTION_CONFIG] ?: defaultConnectionConfig
        set(value) {
            field = value
            pdc[CONNECTION_CONFIG] = value
            blockEntity.setChanged()
        }
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType> by lazy {
        containerConfig.values.associateWith { NetworkConnectionType.BUFFER }
    }
    
    override var insertFilters: CubeFaceMap<ItemFilter<*>?> =
        pdc[INSERT_FILTERS] ?: CubeFaceMap.NULL
        set(value) {
            field = value
            pdc[INSERT_FILTERS] = value
            blockEntity.setChanged()
        }
    
    override var extractFilters: CubeFaceMap<ItemFilter<*>?> =
        pdc[EXTRACT_FILTERS] ?: CubeFaceMap.NULL
        set(value) {
            field = value
            pdc[EXTRACT_FILTERS] = value
            blockEntity.setChanged()
        }
    
    override var insertPriorities: CubeFaceMap<Int> =
        pdc[INSERT_PRIORITIES] ?: DEFAULT_PRIORITIES
        set(value) {
            field = value
            pdc[INSERT_PRIORITIES] = value
            blockEntity.setChanged()
        }
    
    override var extractPriorities: CubeFaceMap<Int> =
        pdc[EXTRACT_PRIORITIES] ?: DEFAULT_PRIORITIES
        set(value) {
            field = value
            pdc[EXTRACT_PRIORITIES] = value
            blockEntity.setChanged()
        }
    
    override var channels: CubeFaceMap<Int> =
        pdc[CHANNELS] ?: DEFAULT_CHANNEL_CONFIG
        set(value) {
            field = value
            pdc[CHANNELS] = value
            blockEntity.setChanged()
        }
    
    private companion object {
        val CONNECTION_CONFIG = Key.key("nova", "item_connection_config")
        val INSERT_FILTERS = Key.key("nova", "item_insert_filters")
        val EXTRACT_FILTERS = Key.key("nova", "item_extract_filters")
        val INSERT_PRIORITIES = Key.key("nova", "item_insert_priorities")
        val EXTRACT_PRIORITIES = Key.key("nova", "item_extract_priorities")
        val CHANNELS = Key.key("nova", "item_channels")
    }
    
}

internal class StaticVanillaItemHolder(
    blockEntity: BlockEntity,
    override var containerConfig: CubeFaceMap<NetworkedInventory?>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : VanillaItemHolder(blockEntity, defaultConnectionConfig) {
    
    override val blockedFaces: CubeFaceSet
        get() = CubeFaceSet.NONE
    
}

internal class DynamicVanillaItemHolder(
    blockEntity: BlockEntity,
    private val inventoriesGetter: () -> CubeFaceMap<NetworkedInventory>,
    private val allowedConnectionTypesGetter: () -> Map<NetworkedInventory, NetworkConnectionType>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> = ALL_BUFFER
) : VanillaItemHolder(blockEntity, defaultConnectionConfig) {
    
    override val blockedFaces: CubeFaceSet
        get() = CubeFaceSet.NONE
    
    override var containerConfig: CubeFaceMap<NetworkedInventory?>
        get() = inventoriesGetter()
        set(_) {}
    
    override val containers: Map<NetworkedInventory, NetworkConnectionType>
        get() = allowedConnectionTypesGetter()
    
}
