package xyz.xenondevs.nova.tileentity.network.node

import java.util.*

/**
 * Used to mark container-like classes that can be used in [ContainerEndPointDataHolders][ContainerEndPointDataHolder].
 * 
 * @see NetworkedInventory
 * @see NetworkedFluidContainer
 */
interface EndPointContainer {
    
    /**
     * The unique identifier of this [EndPointContainer].
     */
    val uuid: UUID
    
}