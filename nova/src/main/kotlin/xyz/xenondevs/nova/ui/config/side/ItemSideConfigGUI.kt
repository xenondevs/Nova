package xyz.xenondevs.nova.ui.config.side

import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory

internal fun ItemSideConfigGUI(
    holder: ItemHolder,
    inventories: List<Pair<NetworkedInventory, String>>
): ItemSideConfigGUI {
    val mergedInventory = holder.mergedInventory
    return if (mergedInventory != null)
        ItemSideConfigGUI(holder, inventories + (mergedInventory to "inventory.nova.all"), mergedInventory)
    else ItemSideConfigGUI(holder, inventories, null)
}

internal class ItemSideConfigGUI(
    holder: ItemHolder,
    inventories: List<Pair<NetworkedInventory, String>>,
    private val mergedInventory: NetworkedInventory?
) : ContainerSideConfigGUI<NetworkedInventory, ItemHolder>(holder, inventories) {
    
    override val hasSimpleVersion: Boolean = mergedInventory != null
    
    init {
        initGUI()
    }
    
    override fun isSimpleConfiguration(): Boolean {
        return hasSimpleVersion && holder.containerConfig.values.all { it == mergedInventory }
    }
    
}