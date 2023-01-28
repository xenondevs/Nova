package xyz.xenondevs.nova.ui.config.side

import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.builder.GuiBuilder
import xyz.xenondevs.invui.gui.builder.guitype.GuiType
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.builder.WindowType
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.item.ClickyTabItem
import xyz.xenondevs.nova.util.playClickSound

class SideConfigGui(
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
    
    private val energyConfigGui: EnergySideConfigGui?
    private val itemConfigGui: ItemSideConfigGui?
    private val fluidConfigGui: FluidSideConfigGui?
    
    private val mainGui: Gui
    
    init {
        val energyHolder = endPoint.holders[NetworkType.ENERGY]
        energyConfigGui = if (energyHolder is EnergyHolder)
            EnergySideConfigGui(energyHolder)
        else null
        
        val itemHolder = endPoint.holders[NetworkType.ITEMS]
        itemConfigGui = if (itemHolder is ItemHolder && inventoryNames != null)
            ItemSideConfigGui(itemHolder, inventoryNames)
        else null
        
        val fluidHolder = endPoint.holders[NetworkType.FLUID]
        fluidConfigGui = if (fluidHolder is FluidHolder && fluidContainerNames != null)
            FluidSideConfigGui(fluidHolder, fluidContainerNames)
        else null
        
        require(energyConfigGui != null || itemConfigGui != null || fluidConfigGui != null)
        
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
                (if (energyConfigGui != null) {
                    if (it.currentTab == 0)
                        CoreGuiMaterial.ENERGY_BTN_SELECTED
                    else CoreGuiMaterial.ENERGY_BTN_ON
                } else CoreGuiMaterial.ENERGY_BTN_OFF).clientsideProvider
            })
            .addIngredient('i', ClickyTabItem(1) {
                (if (itemConfigGui != null) {
                    if (it.currentTab == 1)
                        CoreGuiMaterial.ITEM_BTN_SELECTED
                    else CoreGuiMaterial.ITEM_BTN_ON
                } else CoreGuiMaterial.ITEM_BTN_OFF).clientsideProvider
            })
            .addIngredient('f', ClickyTabItem(2) {
                (if (fluidConfigGui != null) {
                    if (it.currentTab == 2)
                        CoreGuiMaterial.FLUID_BTN_SELECTED
                    else CoreGuiMaterial.FLUID_BTN_ON
                } else CoreGuiMaterial.FLUID_BTN_OFF).clientsideProvider
            })
            .setContent(listOf(energyConfigGui, itemConfigGui, fluidConfigGui))
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

class OpenSideConfigItem(private val sideConfigGui: SideConfigGui) : SimpleItem(CoreGuiMaterial.SIDE_CONFIG_BTN.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        sideConfigGui.openWindow(player)
    }
    
}

class BackItem(private val openPrevious: (Player) -> Unit) : SimpleItem(CoreGuiMaterial.ARROW_1_LEFT.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        openPrevious(player)
    }
    
}