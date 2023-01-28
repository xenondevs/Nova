package xyz.xenondevs.nova.ui.config.side

import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.type.WindowType
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.item.ClickyTabItem
import xyz.xenondevs.nova.util.playClickSound

class SideConfigGUI(
    endPoint: NetworkEndPoint,
    inventoryNames: List<Pair<NetworkedInventory, String>>? = null,
    fluidContainerNames: List<Pair<FluidContainer, String>>? = null,
    openPrevious: (Player) -> Unit
) {
    
    constructor(
        endPoint: NetworkEndPoint,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, null, null, openPrevious)
    
    constructor(
        endPoint: NetworkEndPoint,
        inventories: List<Pair<NetworkedInventory, String>>?,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, inventories, null, openPrevious)
    
    private val energyConfigGUI: EnergySideConfigGUI?
    private val itemConfigGUI: ItemSideConfigGUI?
    private val fluidConfigGUI: FluidSideConfigGUI?
    
    private val mainGui: Gui
    
    init {
        val energyHolder = endPoint.holders[NetworkType.ENERGY]
        energyConfigGUI = if (energyHolder is EnergyHolder)
            EnergySideConfigGUI(energyHolder)
        else null
        
        val itemHolder = endPoint.holders[NetworkType.ITEMS]
        itemConfigGUI = if (itemHolder is ItemHolder && inventoryNames != null)
            ItemSideConfigGUI(itemHolder, inventoryNames)
        else null
        
        val fluidHolder = endPoint.holders[NetworkType.FLUID]
        fluidConfigGUI = if (fluidHolder is FluidHolder && fluidContainerNames != null)
            FluidSideConfigGUI(fluidHolder, fluidContainerNames)
        else null
        
        require(energyConfigGUI != null || itemConfigGUI != null || fluidConfigGUI != null)
        
        mainGui = GuiBuilder(GuiType.TAB)
            .setStructure(
                "< # # e i f # # #",
                "- - - - - - - - -",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x"
            )
            .addIngredient('<', BackItem(openPrevious))
            .addIngredient('e', ClickyTabItem(0) {
                (if (energyConfigGUI != null) {
                    if (it.currentTab == 0)
                        CoreGUIMaterial.ENERGY_BTN_SELECTED
                    else CoreGUIMaterial.ENERGY_BTN_ON
                } else CoreGUIMaterial.ENERGY_BTN_OFF).clientsideProvider
            })
            .addIngredient('i', ClickyTabItem(1) {
                (if (itemConfigGUI != null) {
                    if (it.currentTab == 1)
                        CoreGUIMaterial.ITEM_BTN_SELECTED
                    else CoreGUIMaterial.ITEM_BTN_ON
                } else CoreGUIMaterial.ITEM_BTN_OFF).clientsideProvider
            })
            .addIngredient('f', ClickyTabItem(2) {
                (if (fluidConfigGUI != null) {
                    if (it.currentTab == 2)
                        CoreGUIMaterial.FLUID_BTN_SELECTED
                    else CoreGUIMaterial.FLUID_BTN_ON
                } else CoreGUIMaterial.FLUID_BTN_OFF).clientsideProvider
            })
            .setContent(listOf(energyConfigGUI, itemConfigGUI, fluidConfigGUI))
            .build()
    }
    
    fun openWindow(player: Player) {
        WindowType.NORMAL.createWindow { 
            it.setViewer(player)
            it.setTitle(arrayOf(TranslatableComponent("menu.nova.side_config")))
            it.setGui(mainGui)
        }.show()
    }
    
}

class OpenSideConfigItem(private val sideConfigGUI: SideConfigGUI) : SimpleItem(CoreGUIMaterial.SIDE_CONFIG_BTN.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        sideConfigGUI.openWindow(player)
    }
    
}

class BackItem(private val openPrevious: (Player) -> Unit) : SimpleItem(CoreGUIMaterial.ARROW_1_LEFT.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        openPrevious(player)
    }
    
}