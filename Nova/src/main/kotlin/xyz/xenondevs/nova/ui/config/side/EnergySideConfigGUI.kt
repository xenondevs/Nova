package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.gui.structure.Structure
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.data.addLocalizedLoreLines
import xyz.xenondevs.nova.util.data.setLocalizedName

class EnergySideConfigGUI(
    val energyHolder: EnergyHolder
) : SimpleGUI(9, 3) {
    
    private val structure = Structure("" +
        "# # # # u # # # #" +
        "# # # l f r # # #" +
        "# # # # d b # # #")
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
        NetworkManager.execute {
            it.removeEndPoint(energyHolder.endPoint, false)
            
            val allowedTypes = energyHolder.allowedConnectionType.included
            val currentType = energyHolder.connectionConfig[blockFace]!!
            var index = allowedTypes.indexOf(currentType)
            if (forward) index++ else index--
            if (index < 0) index = allowedTypes.lastIndex
            else if (index == allowedTypes.size) index = 0
            energyHolder.connectionConfig[blockFace] = allowedTypes[index]
            
            it.addEndPoint(energyHolder.endPoint, false)
                .thenRun { energyHolder.endPoint.updateNearbyBridges() }
        }
    }
    
    private inner class SideConfigItem(val blockSide: BlockSide) : BaseItem() {
        
        private val blockFace = (energyHolder.endPoint as TileEntity).getFace(blockSide)
        
        override fun getItemProvider(): ItemProvider {
            val blockSide = blockSide.name[0] + blockSide.name.substring(1).lowercase()
            return when (energyHolder.connectionConfig[blockFace]!!) {
                NetworkConnectionType.NONE ->
                    CoreGUIMaterial.GRAY_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GRAY, "menu.nova.side_config.none")
                NetworkConnectionType.EXTRACT ->
                    CoreGUIMaterial.ORANGE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GOLD, "menu.nova.side_config.output")
                NetworkConnectionType.INSERT ->
                    CoreGUIMaterial.BLUE_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.AQUA, "menu.nova.side_config.input")
                NetworkConnectionType.BUFFER ->
                    CoreGUIMaterial.GREEN_BTN.createClientsideItemBuilder().addLocalizedLoreLines(ChatColor.GREEN, "menu.nova.side_config.input_output")
            }.setLocalizedName(ChatColor.GRAY, "menu.nova.side_config.${blockSide.lowercase()}")
        }
        
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            changeConnectionType(blockFace, clickType.isLeftClick)
            notifyWindows()
        }
        
    }
    
}