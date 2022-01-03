package xyz.xenondevs.nova.ui.config.side

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
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
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.item.ClickyTabItem

class SideConfigGUI(
    endPoint: NetworkEndPoint,
    allowedEnergyTypes: List<EnergyConnectionType>? = null,
    inventories: List<Pair<NetworkedInventory, String>>? = null,
    fluidContainers: List<Pair<FluidContainer, String>>? = null,
    openPrevious: (Player) -> Unit
) {
    
    constructor(
        endPoint: NetworkEndPoint,
        allowedEnergyTypes: List<EnergyConnectionType>?,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, allowedEnergyTypes, null, null, openPrevious)
    
    constructor(
        endPoint: NetworkEndPoint,
        allowedEnergyTypes: List<EnergyConnectionType>?,
        inventories: List<Pair<NetworkedInventory, String>>?,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, allowedEnergyTypes, inventories, null, openPrevious)
    
    private val energyConfigGUI = if (allowedEnergyTypes != null)
        EnergySideConfigGUI(endPoint.holders[NetworkType.ENERGY] as EnergyHolder, allowedEnergyTypes) else null
    
    private val itemConfigGUI = if (inventories != null)
        ItemSideConfigGUI(endPoint.holders[NetworkType.ITEMS] as ItemHolder, inventories) else null
    
    private val fluidConfigGUI = if (fluidContainers != null)
        FluidSideConfigGUI(endPoint.holders[NetworkType.FLUID] as FluidHolder, fluidContainers) else null
    
    private val mainGUI: GUI
    
    init {
        require(energyConfigGUI != null || itemConfigGUI != null || fluidConfigGUI != null)
        
        mainGUI = GUIBuilder(GUIType.TAB, 9, 5)
            .setStructure("" +
                "< # # e i f # # #" +
                "- - - - - - - - -" +
                "x x x x x x x x x" +
                "x x x x x x x x x" +
                "x x x x x x x x x")
            .addIngredient('<', BackItem(openPrevious))
            .addIngredient('e', ClickyTabItem(0) {
                (if (energyConfigGUI != null) {
                    if (it.currentTab == 0)
                        NovaMaterialRegistry.ENERGY_SELECTED_BUTTON
                    else NovaMaterialRegistry.ENERGY_ON_BUTTON
                } else NovaMaterialRegistry.ENERGY_OFF_BUTTON).itemProvider
            })
            .addIngredient('i', ClickyTabItem(1) {
                (if (itemConfigGUI != null) {
                    if (it.currentTab == 1)
                        NovaMaterialRegistry.ITEM_SELECTED_BUTTON
                    else NovaMaterialRegistry.ITEM_ON_BUTTON
                } else NovaMaterialRegistry.ITEM_OFF_BUTTON).itemProvider
            })
            .addIngredient('f', ClickyTabItem(2) {
                (if (fluidConfigGUI != null) {
                    if (it.currentTab == 2)
                        NovaMaterialRegistry.FLUID_SELECTED_BUTTON
                    else NovaMaterialRegistry.FLUID_ON_BUTTON
                } else NovaMaterialRegistry.FLUID_OFF_BUTTON).itemProvider
            })
            .setGUIs(listOf(energyConfigGUI, itemConfigGUI, fluidConfigGUI))
            .build()
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