package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.util.BlockSide

internal abstract class InventorySideConfigGUI(
    holder: EndPointDataHolder
) : BaseSideConfigGUI(holder) {
    
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
    
    protected abstract fun changeInventory(blockFace: BlockFace, forward: Boolean): Boolean
    protected abstract fun getInventoryButtonBuilder(blockFace: BlockFace): ItemBuilder
    
    private inner class InventoryConfigItem(blockSide: BlockSide) : ConfigItem(blockSide) {
        
        override fun getItemProvider(): ItemProvider {
            return getInventoryButtonBuilder(blockFace).setDisplayName(getSideName(blockSide, blockFace))
        }
        
    }
    
}