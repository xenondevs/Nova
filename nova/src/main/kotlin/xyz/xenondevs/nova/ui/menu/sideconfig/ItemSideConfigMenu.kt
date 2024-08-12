package xyz.xenondevs.nova.ui.menu.sideconfig

import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory

internal fun ItemSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: ItemHolder,
    namedInventories: Map<NetworkedInventory, String>
): ItemSideConfigMenu {
    val mergedInventory = holder.mergedInventory
    return if (mergedInventory != null)
        ItemSideConfigMenu(endPoint, holder, namedInventories + (mergedInventory to "inventory.nova.all"), mergedInventory)
    else ItemSideConfigMenu(endPoint, holder, namedInventories, null)
}

internal class ItemSideConfigMenu(
    endPoint: NetworkEndPoint,
    holder: ItemHolder,
    namedInventories: Map<NetworkedInventory, String>,
    private val mergedInventory: NetworkedInventory?
) : ContainerSideConfigMenu<NetworkedInventory, ItemHolder>(endPoint, DefaultNetworkTypes.ITEM, holder, namedInventories) {
    
    override val hasSimpleVersion: Boolean = mergedInventory != null
    override val hasAdvancedVersion: Boolean = mergedInventory == null || namedInventories.size > 2
    
    init {
        require(namedInventories.isNotEmpty())
    }
    
    override fun isSimpleConfiguration(): Boolean {
        return !hasAdvancedVersion || (hasSimpleVersion && holder.containerConfig.values.all { it == mergedInventory })
    }
    
}