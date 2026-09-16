package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder

import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.entity.BlockEntity
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_CHANNEL_CONFIG
import xyz.xenondevs.nova.world.block.tileentity.network.node.DefaultContainerEndPointDataHolder.Companion.DEFAULT_PRIORITIES
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer

internal class VanillaFluidHolder(
    private val blockEntity: BlockEntity,
    override val containers: Map<NetworkedFluidContainer, NetworkConnectionType>,
    private val fixedContainerConfig: CubeFaceMap<NetworkedFluidContainer?>,
    defaultConnectionConfig: CubeFaceMap<NetworkConnectionType> =
        fixedContainerConfig.map { containers[it] ?: NetworkConnectionType.NONE }
) : FluidHolder {
    
    private val pdc = blockEntity.persistentDataContainer
    
    override val blockedFaces = CubeFaceSet.NONE
    
    init {
        require(containers.isNotEmpty())
    }
    
    override var containerConfig: CubeFaceMap<NetworkedFluidContainer?>
        get() = fixedContainerConfig
        set(value) {
            require(value == fixedContainerConfig) { "Vanilla fluid container layout cannot be changed" }
        }
    
    override var connectionConfig: CubeFaceMap<NetworkConnectionType> =
        pdc[CONNECTION_CONFIG] ?: defaultConnectionConfig
        set(value) {
            field = value
            pdc[CONNECTION_CONFIG] = value
            blockEntity.setChanged()
        }
    
    override var channels: CubeFaceMap<Int> =
        pdc[CHANNELS] ?: DEFAULT_CHANNEL_CONFIG
        set(value) {
            field = value
            pdc[CHANNELS] = value
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
    
    private companion object {
        
        val CONNECTION_CONFIG = Key.key("nova", "fluid_connection_config")
        val CHANNELS = Key.key("nova", "fluid_channels")
        val INSERT_PRIORITIES = Key.key("nova", "fluid_insert_priorities")
        val EXTRACT_PRIORITIES = Key.key("nova", "fluid_extract_priorities")
        
    }
    
}
