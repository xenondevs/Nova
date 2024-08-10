package xyz.xenondevs.nova.world.block.tileentity.network.node

import java.util.*

/**
 * Used to mark container-like classes that can be used in [ContainerEndPointDataHolders][ContainerEndPointDataHolder].
 */
interface EndPointContainer {
    
    /**
     * The unique identifier of this [EndPointContainer].
     */
    val uuid: UUID
    
}