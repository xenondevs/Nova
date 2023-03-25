package xyz.xenondevs.nova.ui.config.side

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.material.DefaultGuiMaterial
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.item.BackItem
import xyz.xenondevs.nova.ui.item.ClickyTabItem
import xyz.xenondevs.nova.util.playClickSound

class SideConfigMenu(
    endPoint: NetworkEndPoint,
    inventoryNames: List<Pair<NetworkedInventory, String>>? = null,
    fluidContainerNames: List<Pair<FluidContainer, String>>? = null,
    openPrevious: (Player) -> Unit
) {
    
    constructor(
        endPoint: NetworkEndPoint,
        inventoryNames: List<Pair<NetworkedInventory, String>>? = null,
        fluidContainerNames: List<Pair<FluidContainer, String>>? = null,
        openPrevious: () -> Unit
    ) : this(endPoint, inventoryNames, fluidContainerNames, { _ -> openPrevious() })
    
    constructor(
        endPoint: NetworkEndPoint,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, null, null, openPrevious)
    
    constructor(
        endPoint: NetworkEndPoint,
        openPrevious: () -> Unit
    ) : this(endPoint, null, null, { _ -> openPrevious() })
    
    constructor(
        endPoint: NetworkEndPoint,
        inventories: List<Pair<NetworkedInventory, String>>?,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, inventories, null, openPrevious)
    
    constructor(
        endPoint: NetworkEndPoint,
        inventories: List<Pair<NetworkedInventory, String>>?,
        openPrevious: () -> Unit
    ) : this(endPoint, inventories, null, { _ -> openPrevious() })
    
    private val energyConfigGui: EnergySideConfigGui?
    private val itemConfigGui: ItemSideConfigGui?
    private val fluidConfigGui: FluidSideConfigGui?
    
    private val mainGui: Gui
    
    init {
        val energyHolder = endPoint.holders[DefaultNetworkTypes.ENERGY]
        energyConfigGui = if (energyHolder is EnergyHolder)
            EnergySideConfigGui(energyHolder)
        else null
        
        val itemHolder = endPoint.holders[DefaultNetworkTypes.ITEMS]
        itemConfigGui = if (itemHolder is ItemHolder && inventoryNames != null)
            ItemSideConfigGui(itemHolder, inventoryNames)
        else null
        
        val fluidHolder = endPoint.holders[DefaultNetworkTypes.FLUID]
        fluidConfigGui = if (fluidHolder is FluidHolder && fluidContainerNames != null)
            FluidSideConfigGui(fluidHolder, fluidContainerNames)
        else null
        
        require(energyConfigGui != null || itemConfigGui != null || fluidConfigGui != null)
        
        mainGui = TabGui.normal()
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
                        DefaultGuiMaterial.ENERGY_BTN_SELECTED
                    else DefaultGuiMaterial.ENERGY_BTN_ON
                } else DefaultGuiMaterial.ENERGY_BTN_OFF).clientsideProvider
            })
            .addIngredient('i', ClickyTabItem(1) {
                (if (itemConfigGui != null) {
                    if (it.currentTab == 1)
                        DefaultGuiMaterial.ITEM_BTN_SELECTED
                    else DefaultGuiMaterial.ITEM_BTN_ON
                } else DefaultGuiMaterial.ITEM_BTN_OFF).clientsideProvider
            })
            .addIngredient('f', ClickyTabItem(2) {
                (if (fluidConfigGui != null) {
                    if (it.currentTab == 2)
                        DefaultGuiMaterial.FLUID_BTN_SELECTED
                    else DefaultGuiMaterial.FLUID_BTN_ON
                } else DefaultGuiMaterial.FLUID_BTN_OFF).clientsideProvider
            })
            .setTabs(listOf(energyConfigGui, itemConfigGui, fluidConfigGui))
            .build()
    }
    
    fun openWindow(player: Player) {
        Window.single {
            it.setViewer(player)
            it.setTitle(Component.translatable("menu.nova.side_config"))
            it.setGui(mainGui)
        }.open()
    }
    
}

class OpenSideConfigItem(private val sideConfigMenu: SideConfigMenu) : SimpleItem(DefaultGuiMaterial.SIDE_CONFIG_BTN.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        sideConfigMenu.openWindow(player)
    }
    
}