package xyz.xenondevs.nova.ui.config.side

import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

internal fun ItemSideConfigGui(
    holder: ItemHolder,
    inventories: List<Pair<NetworkedInventory, String>>
): ItemSideConfigGui {
    val mergedInventory = holder.mergedInventory
    return if (mergedInventory != null)
        ItemSideConfigGui(holder, inventories + (mergedInventory to "inventory.nova.all"), mergedInventory)
    else ItemSideConfigGui(holder, inventories, null)
}

internal class ItemSideConfigGui(
    holder: ItemHolder,
    inventories: List<Pair<NetworkedInventory, String>>,
    private val mergedInventory: NetworkedInventory?
) : ContainerSideConfigGui<NetworkedInventory, ItemHolder>(holder, inventories) {
    
    override val hasSimpleVersion: Boolean = mergedInventory != null
    override val hasAdvancedVersion: Boolean = mergedInventory == null || inventories.size > 2
    
    init {
        require(inventories.isNotEmpty())
        initGui()
    }
    
    override fun isSimpleConfiguration(): Boolean {
        return !hasAdvancedVersion || (hasSimpleVersion && holder.containerConfig.values.all { it == mergedInventory })
    }
    
}