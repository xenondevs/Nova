package xyz.xenondevs.nova.ui.config

import com.google.common.base.Preconditions
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.gui.impl.SimpleGUI
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyStorage
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemStorage
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.item.ClickyTabItem
import xyz.xenondevs.nova.util.data.setLocalizedName

class SideConfigGUI(
    endPoint: NetworkEndPoint,
    allowedEnergyTypes: List<EnergyConnectionType>?,
    inventories: List<Triple<NetworkedInventory, String, List<ItemConnectionType>>>?,
    openPrevious: (Player) -> Unit
) {
    
    private val energyConfigGUI = if (allowedEnergyTypes != null)
        EnergySideConfigGUI(endPoint as EnergyStorage, allowedEnergyTypes) else null
    
    private val itemConfigGUI = if (inventories != null)
        ItemSideConfigGUI(endPoint as ItemStorage, inventories) else null
    
    private val mainGUI: GUI
    
    init {
        Preconditions.checkArgument(energyConfigGUI != null || itemConfigGUI != null)
        
        if (energyConfigGUI != null && itemConfigGUI != null) {
            mainGUI = GUIBuilder(GUIType.TAB, 9, 3)
                .setStructure("" +
                    "b x x x x x x x x" +
                    "e x x x x x x x x" +
                    "i x x x x x x x x")
                .addIngredient('b', BackItem(openPrevious))
                .addIngredient('e', ClickyTabItem(0) {
                    if (it.currentTab == 0) NovaMaterialRegistry.ENERGY_OFF_BUTTON.createBasicItemBuilder()
                    else NovaMaterialRegistry.ENERGY_ON_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.side_config.energy")
                })
                .addIngredient('i', ClickyTabItem(1) {
                    if (it.currentTab == 1) NovaMaterialRegistry.ITEM_OFF_BUTTON.createBasicItemBuilder()
                    else NovaMaterialRegistry.ITEM_ON_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.side_config.item")
                })
                .addGUI(energyConfigGUI)
                .addGUI(itemConfigGUI)
                .build()
        } else {
            val chosenGUI = energyConfigGUI ?: itemConfigGUI!!
            mainGUI = SimpleGUI(9, 3)
            mainGUI.setItem(0, BackItem(openPrevious))
            mainGUI.fillRectangle(1, 0, chosenGUI, true)
            mainGUI.fill(SimpleItem(Icon.BACKGROUND.itemBuilder), false)
        }
    }
    
    fun openWindow(player: Player) {
        SimpleWindow(player, arrayOf(TranslatableComponent("menu.nova.side_config")), mainGUI).show()
    }
    
}

class OpenSideConfigItem(private val sideConfigGUI: SideConfigGUI) : SimpleItem(NovaMaterialRegistry.SIDE_CONFIG_BUTTON.item.createItemBuilder("menu.nova.side_config")) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        sideConfigGUI.openWindow(player)
    }
    
}

class BackItem(private val openPrevious: (Player) -> Unit) : SimpleItem(Icon.ARROW_1_LEFT.itemBuilder) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        openPrevious(player)
    }
    
}