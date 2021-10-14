package xyz.xenondevs.nova.ui.config

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.enumMapOf

val BUTTON_COLORS = listOf(
    NovaMaterialRegistry.RED_BUTTON,
    NovaMaterialRegistry.ORANGE_BUTTON,
    NovaMaterialRegistry.YELLOW_BUTTON,
    NovaMaterialRegistry.GREEN_BUTTON,
    NovaMaterialRegistry.BLUE_BUTTON,
    NovaMaterialRegistry.PINK_BUTTON,
    NovaMaterialRegistry.WHITE_BUTTON
)

class ItemSideConfigGUI(
    val itemHolder: ItemHolder,
    inventories: List<Pair<NetworkedInventory, String>>
) : SimpleGUI(8, 3) {
    
    private val inventories = inventories.map { it.first }
    private val allowedTypes = itemHolder.allowedConnectionTypes.mapValues { (_, type) -> type.included }
    private val buttonBuilders = inventories.withIndex().associate { (index, triple) ->
        triple.first to BUTTON_COLORS[index]
            .createBasicItemBuilder()
            .addLoreLines(TranslatableComponent(triple.second).apply { color = ChatColor.AQUA })
    }
    
    private val configItems = enumMapOf<BlockFace, MutableList<Item>>()
    
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
    
    private fun registerConfigItem(blockFace: BlockFace, item: Item) {
        val configItemList = configItems[blockFace] ?: ArrayList<Item>().also { configItems[blockFace] = it }
        configItemList.add(item)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean {
        val allowedTypes = allowedTypes[itemHolder.inventories[blockFace]!!]!!
        if (allowedTypes.size < 2) return false
        
        NetworkManager.handleEndPointRemove(itemHolder.endPoint, true)
        
        val currentType = itemHolder.itemConfig[blockFace]!!
        var index = allowedTypes.indexOf(currentType)
        if (forward) index++ else index--
        if (index < 0) index = allowedTypes.lastIndex
        else if (index == allowedTypes.size) index = 0
        itemHolder.itemConfig[blockFace] = allowedTypes[index]
        
        NetworkManager.handleEndPointAdd(itemHolder.endPoint, false)
        itemHolder.endPoint.updateNearbyBridges()
        
        return true
    }
    
    private fun changeInventory(blockFace: BlockFace, forward: Boolean): Boolean {
        if (inventories.size < 2) return false
        
        NetworkManager.handleEndPointRemove(itemHolder.endPoint, false)
        
        val currentInventory = itemHolder.inventories[blockFace]!!
        var index = inventories.indexOf(currentInventory)
        if (forward) index++ else index--
        if (index < 0) index = inventories.lastIndex
        else if (index == inventories.size) index = 0
        
        val newInventory = inventories[index]
        itemHolder.inventories[blockFace] = newInventory
        
        val allowedTypes = allowedTypes[newInventory]!!
        if (!allowedTypes.contains(itemHolder.itemConfig[blockFace]!!)) {
            itemHolder.itemConfig[blockFace] = allowedTypes[0]
        }
        
        NetworkManager.handleEndPointAdd(itemHolder.endPoint)
        
        return true
    }
    
    private inner class ConnectionConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (itemHolder.endPoint as TileEntity).getFace(blockSide)
        
        init {
            registerConfigItem(blockFace, this)
        }
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (itemHolder.itemConfig[blockFace]!!) {
                ItemConnectionType.NONE ->
                    NovaMaterialRegistry.GRAY_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.none")
                ItemConnectionType.EXTRACT ->
                    NovaMaterialRegistry.ORANGE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.output")
                ItemConnectionType.INSERT ->
                    NovaMaterialRegistry.BLUE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input")
                ItemConnectionType.BUFFER ->
                    NovaMaterialRegistry.GREEN_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input_output")
            }.setLocalizedName("menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeConnectionType(blockFace, clickType.isLeftClick)) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                configItems[blockFace]?.forEach(Item::notifyWindows)
            }
        }
        
    }
    
    private inner class InventoryConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (itemHolder.endPoint as TileEntity).getFace(blockSide)
        
        init {
            registerConfigItem(blockFace, this)
        }
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            val inventory = itemHolder.inventories[blockFace]!!
            return buttonBuilders[inventory]!!.clone().setLocalizedName("menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeInventory(blockFace, clickType.isLeftClick)) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                configItems[blockFace]?.forEach(Item::notifyWindows)
            }
        }
        
    }
    
}