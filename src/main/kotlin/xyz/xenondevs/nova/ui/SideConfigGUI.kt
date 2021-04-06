package xyz.xenondevs.nova.ui

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.energy.EnergyConnectionType
import xyz.xenondevs.nova.energy.EnergyNetworkManager
import xyz.xenondevs.nova.energy.EnergyStorage
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.BlockSide

class SideConfigGUI(
    val energyStorage: EnergyStorage,
    vararg val allowedTypes: EnergyConnectionType,
    val openPrevious: (Player) -> Unit,
    ) : SimpleGUI(9, 3) {
    
    private val structure = Structure("" +
        "~ # # # u # # # #" +
        "# # # l f r # # #" +
        "# # # # d b # # #")
        .addIngredient('u', SideConfigItem(BlockSide.TOP))
        .addIngredient('l', SideConfigItem(BlockSide.LEFT))
        .addIngredient('f', SideConfigItem(BlockSide.FRONT))
        .addIngredient('r', SideConfigItem(BlockSide.RIGHT))
        .addIngredient('d', SideConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', SideConfigItem(BlockSide.BACK))
        .addIngredient('~', BackItem())
    
    init {
        applyStructure(structure)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean) {
        EnergyNetworkManager.handleStorageRemove(energyStorage, false)
        
        val currentType = energyStorage.configuration[blockFace]!!
        var index = allowedTypes.indexOf(currentType)
        if (forward) index++ else index--
        if (index < 0) index = allowedTypes.lastIndex
        else if (index == allowedTypes.size) index = 0
        energyStorage.configuration[blockFace] = allowedTypes[index]
        
        EnergyNetworkManager.handleStorageAdd(energyStorage)
    }
    
    private inner class SideConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (energyStorage as TileEntity).getFace(blockSide)
        
        override fun getItemBuilder(): ItemBuilder {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).toLowerCase()
            return when (energyStorage.configuration[blockFace]!!) {
                EnergyConnectionType.NONE ->
                    NovaMaterial.GRAY_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§7None")
                EnergyConnectionType.PROVIDE ->
                    NovaMaterial.ORANGE_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§6Output")
                EnergyConnectionType.CONSUME ->
                    NovaMaterial.BLUE_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§bInput")
                EnergyConnectionType.BUFFER ->
                    NovaMaterial.GREEN_BUTTON.createItemBuilder().setDisplayName("§7$blockSide").addLoreLines("§aInput & Output")
            }
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeConnectionType(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
    private inner class BackItem : SimpleItem(Icon.ARROW_1_LEFT.itemBuilder) {
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            openPrevious(player)
        }
        
    }
    
}

class OpenSideConfigItem(private val sideConfigGUI: SideConfigGUI) : SimpleItem(NovaMaterial.SIDE_CONFIG_BUTTON.item.getItemBuilder("Side Config")) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        SimpleWindow(player, "Side Config", sideConfigGUI).show()
    }
    
}