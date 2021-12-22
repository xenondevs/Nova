package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
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

abstract class InventorySideConfigGUI : SimpleGUI(9, 3) {
    
    private val configItems = enumMapOf<BlockFace, MutableList<Item>>()
    
    protected fun initGUI() {
        val structure = Structure("" +
            "# u # # # # # 1 #" +
            "l f r # # # 2 3 4" +
            "# d b # # # # 5 6")
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
    
    protected abstract fun changeConnectionType(blockFace: BlockFace, forward: Boolean): Boolean
    protected abstract fun changeInventory(blockFace: BlockFace, forward: Boolean): Boolean
    protected abstract fun getBlockFace(blockSide: BlockSide): BlockFace
    protected abstract fun getConnectionType(blockFace: BlockFace): NetworkConnectionType
    protected abstract fun getInventoryButtonBuilder(blockFace: BlockFace): ItemBuilder
    
    private inner class ConnectionConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = getBlockFace(blockSide)
        
        init {
            registerConfigItem(blockFace, this)
        }
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (getConnectionType(blockFace)) {
                NetworkConnectionType.NONE ->
                    NovaMaterialRegistry.GRAY_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.none")
                NetworkConnectionType.EXTRACT ->
                    NovaMaterialRegistry.ORANGE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.output")
                NetworkConnectionType.INSERT ->
                    NovaMaterialRegistry.BLUE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input")
                NetworkConnectionType.BUFFER ->
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
        
        private val blockFace = getBlockFace(blockSide)
        
        init {
            registerConfigItem(blockFace, this)
        }
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return getInventoryButtonBuilder(blockFace).setLocalizedName("menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            if (changeInventory(blockFace, clickType.isLeftClick)) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                configItems[blockFace]?.forEach(Item::notifyWindows)
            }
        }
        
    }
    
}