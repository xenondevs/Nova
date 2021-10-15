package xyz.xenondevs.nova.ui.config

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.setLocalizedName

class EnergySideConfigGUI(
    val energyHolder: EnergyHolder,
    private val allowedTypes: List<EnergyConnectionType>,
) : SimpleGUI(8, 3) {
    
    private val structure = Structure("" +
        "# # # u # # # #" +
        "# # l f r # # #" +
        "# # # d b # # #")
        .addIngredient('u', SideConfigItem(BlockSide.TOP))
        .addIngredient('l', SideConfigItem(BlockSide.LEFT))
        .addIngredient('f', SideConfigItem(BlockSide.FRONT))
        .addIngredient('r', SideConfigItem(BlockSide.RIGHT))
        .addIngredient('d', SideConfigItem(BlockSide.BOTTOM))
        .addIngredient('b', SideConfigItem(BlockSide.BACK))
    
    init {
        applyStructure(structure)
    }
    
    private fun changeConnectionType(blockFace: BlockFace, forward: Boolean) {
        // TODO: runSync / runAsync ?
        NetworkManager.runNow {
            it.handleEndPointRemove(energyHolder.endPoint, true)
            
            val currentType = energyHolder.energyConfig[blockFace]!!
            var index = allowedTypes.indexOf(currentType)
            if (forward) index++ else index--
            if (index < 0) index = allowedTypes.lastIndex
            else if (index == allowedTypes.size) index = 0
            energyHolder.energyConfig[blockFace] = allowedTypes[index]
            
            it.handleEndPointAdd(energyHolder.endPoint, false)
            energyHolder.endPoint.updateNearbyBridges()
        }
    }
    
    private inner class SideConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = energyHolder.endPoint.getFace(blockSide)
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (energyHolder.energyConfig[blockFace]!!) {
                EnergyConnectionType.NONE ->
                    NovaMaterialRegistry.GRAY_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.none")
                EnergyConnectionType.PROVIDE ->
                    NovaMaterialRegistry.ORANGE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.output")
                EnergyConnectionType.CONSUME ->
                    NovaMaterialRegistry.BLUE_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input")
                EnergyConnectionType.BUFFER ->
                    NovaMaterialRegistry.GREEN_BUTTON.createItemBuilder().addLocalizedLoreLines("menu.nova.side_config.input_output")
            }.setLocalizedName("menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeConnectionType(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
}