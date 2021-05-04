package xyz.xenondevs.nova.ui.config

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.BlockSide

private val BUTTON_COLORS = listOf(NovaMaterial.ORANGE_BUTTON, NovaMaterial.BLUE_BUTTON, NovaMaterial.GREEN_BUTTON)

class ItemSideConfigGUI(
    val itemStorage: ItemStorage,
    private val allowedTypes: List<ItemConnectionType>,
    inventoryNames: List<Pair<NetworkedInventory, String>>
) : SimpleGUI(8, 3) {
    
    private val inventories = inventoryNames.map { it.first }
    private val buttonBuilders: Map<NetworkedInventory, ItemBuilder> =
        inventoryNames.toList().withIndex().associate { (index, pair) ->
            pair.first to BUTTON_COLORS[index].createBasicItemBuilder().addLoreLines("§b${pair.second}")
        }
    
    init {
        val structure = Structure("" +
            "# u # # # # 1 #" +
            "l f r # # 2 3 4" +
            "# d b # # # 5 6")
            .addIngredient('u', ConnectionConfigItem(BlockSide.TOP))
            .addIngredient('l', ConnectionConfigItem(BlockSide.LEFT))
            .addIngredient('f', ConnectionConfigItem(BlockSide.FRONT))
            .addIngredient('r', ConnectionConfigItem(BlockSide.RIGHT))
            .addIngredient('d', ConnectionConfigItem(BlockSide.BOTTOM))
            .addIngredient('b', ConnectionConfigItem(BlockSide.BACK))
            .addIngredient('1', InventoryConfigItem(BlockSide.TOP))
            .addIngredient('2', InventoryConfigItem(BlockSide.LEFT))
            .addIngredient('3', InventoryConfigItem(BlockSide.FRONT))
            .addIngredient('4', InventoryConfigItem(BlockSide.RIGHT))
            .addIngredient('5', InventoryConfigItem(BlockSide.BOTTOM))
            .addIngredient('6', InventoryConfigItem(BlockSide.BACK))
        
        applyStructure(structure)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean) {
        NetworkManager.handleEndPointRemove(itemStorage, false)
        
        val currentType = itemStorage.itemConfig[blockFace]!!
        var index = allowedTypes.indexOf(currentType)
        if (forward) index++ else index--
        if (index < 0) index = allowedTypes.lastIndex
        else if (index == allowedTypes.size) index = 0
        itemStorage.itemConfig[blockFace] = allowedTypes[index]
        
        NetworkManager.handleEndPointAdd(itemStorage)
    }
    
    private fun changeInventory(blockFace: BlockFace, forward: Boolean) {
        NetworkManager.handleEndPointRemove(itemStorage, false)
        
        val currentInventory = itemStorage.inventories[blockFace]!! as NetworkedVirtualInventory
        var index = inventories.indexOf(currentInventory)
        if (forward) index++ else index--
        if (index < 0) index = inventories.lastIndex
        else if (index == inventories.size) index = 0
        itemStorage.inventories[blockFace] = inventories[index]
        
        NetworkManager.handleEndPointAdd(itemStorage)
    }
    
    private inner class ConnectionConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (itemStorage as TileEntity).getFace(blockSide)
        
        override fun getItemBuilder(): ItemBuilder {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (itemStorage.itemConfig[blockFace]!!) {
                ItemConnectionType.NONE ->
                    NovaMaterial.GRAY_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§7None")
                ItemConnectionType.EXTRACT ->
                    NovaMaterial.ORANGE_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§6Output")
                ItemConnectionType.INSERT ->
                    NovaMaterial.BLUE_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§bInput")
                ItemConnectionType.BUFFER ->
                    NovaMaterial.GREEN_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§aInput & Output")
            }
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeConnectionType(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
    private inner class InventoryConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (itemStorage as TileEntity).getFace(blockSide)
        
        override fun getItemBuilder(): ItemBuilder {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            val inventory = itemStorage.inventories[blockFace]!! as NetworkedVirtualInventory
            return buttonBuilders[inventory]!!.clone().setDisplayName("§7$blockSide")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeInventory(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
}