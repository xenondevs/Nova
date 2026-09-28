package xyz.xenondevs.nova.ui.menu.sideconfig

import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.format.NetworkState

fun ItemSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: ItemHolder,
    namedInventories: Map<NetworkedInventory, String>
): ItemSideConfigMenu {
    val mergedInventory = holder.mergedInventory
    return if (mergedInventory != null)
        ItemSideConfigMenu(endPoint, holder, namedInventories + (mergedInventory to "inventory.nova.all"), mergedInventory)
    else ItemSideConfigMenu(endPoint, holder, namedInventories, null)
}

class ItemSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: ItemHolder,
    namedInventories: Map<NetworkedInventory, String>,
    private val mergedInventory: NetworkedInventory?
) : ContainerSideConfigMenu<NetworkedInventory, ItemHolder>(endPoint, DefaultNetworkTypes.ITEM, holder, namedInventories) {
    
    private val hasSimpleVersion = mergedInventory != null
    private val hasAdvancedVersion = mergedInventory == null || namedContainers.size > 2
    
    init {
        require(namedInventories.isNotEmpty())
    }
    
    override fun init(state: NetworkState) {
        super.init(state)
        
        simpleMode.set(when {
            hasSimpleVersion && !hasAdvancedVersion -> SimplicityMode.SIMPLE_ONLY
            !hasSimpleVersion -> SimplicityMode.ADVANCED_ONLY
            isSimpleConfiguration() -> SimplicityMode.SIMPLE
            else -> SimplicityMode.ADVANCED
        })
    }
    
    override fun isSimpleConfiguration(): Boolean {
        return !hasAdvancedVersion || (hasSimpleVersion && holder.containerConfig.values.all { it == mergedInventory })
    }
    
}