package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.menu.item.ClickyTabItem
import xyz.xenondevs.nova.util.playClickSound

/**
 * Creates a new [SideConfigMenu] for [endPoint] using the given
 * [inventories] with their localized names.
 */
@JvmName("SideConfigMenuItem")
fun SideConfigMenu(
    endPoint: NetworkEndPoint,
    inventories: Map<NetworkedInventory, String>,
    openPrevious: (Player) -> Unit
) = SideConfigMenu(endPoint, inventories, null, openPrevious)

/**
 * Creates a new [SideConfigMenu] for [endPoint] using the given
 * [inventories] with their localized names.
 */
@JvmName("SideConfigMenuItem")
fun SideConfigMenu(
    endPoint: NetworkEndPoint,
    inventories: Map<NetworkedInventory, String>,
    openPrevious: () -> Unit
) = SideConfigMenu(endPoint, inventories, null) { _ -> openPrevious() }

/**
 * Creates a new [SideConfigMenu] for [endPoint] using the given
 * [containers] with their localized names.
 */
@JvmName("SideConfigMenuFluid")
fun SideConfigMenu(
    endPoint: NetworkEndPoint,
    containers: Map<NetworkedFluidContainer, String>,
    openPrevious: (Player) -> Unit
) = SideConfigMenu(endPoint, null, containers, openPrevious)

/**
 * Creates a new [SideConfigMenu] for [endPoint] using the given
 * [containers] with their localized names.
 */
@JvmName("SideConfigMenuFluid")
fun SideConfigMenu(
    endPoint: NetworkEndPoint,
    containers: Map<NetworkedFluidContainer, String>,
    openPrevious: () -> Unit
) = SideConfigMenu(endPoint, null, containers) { _ -> openPrevious() }

/**
 * The built-in implementation of a side-config menu that supports all built-in
 * network types (energy, item, fluid).
 */
class SideConfigMenu(
    private val endPoint: NetworkEndPoint,
    inventories: Map<NetworkedInventory, String>? = null,
    containers: Map<NetworkedFluidContainer, String>? = null,
    openPrevious: (Player) -> Unit
) {
    
    /**
     * Creates a new [SideConfigMenu] for [endPoint] using the given
     * [inventories] and [containers] with their localized names.
     */
    constructor(
        endPoint: NetworkEndPoint,
        inventories: Map<NetworkedInventory, String>? = null,
        containers: Map<NetworkedFluidContainer, String>? = null,
        openPrevious: () -> Unit
    ) : this(endPoint, inventories, containers, { _ -> openPrevious() })
    
    /**
     * Creates a new [SideConfigMenu] for [endPoint].
     */
    constructor(
        endPoint: NetworkEndPoint,
        openPrevious: (Player) -> Unit
    ) : this(endPoint, null, null, openPrevious)
    
    /**
     * Creates a new [SideConfigMenu] for [endPoint].
     */
    constructor(
        endPoint: NetworkEndPoint,
        openPrevious: () -> Unit
    ) : this(endPoint, null, null, { _ -> openPrevious() })
    
    private val energyConfigMenu: EnergySideConfigMenu?
    private val itemConfigMenu: ItemSideConfigMenu?
    private val fluidConfigMenu: FluidSideConfigMenu?
    
    private val mainGui: Gui
    
    init {
        val energyHolder = endPoint.holders.firstInstanceOfOrNull<EnergyHolder>()
        energyConfigMenu = if (energyHolder != null)
            EnergySideConfigMenu(endPoint, energyHolder)
        else null
        
        val itemHolder = endPoint.holders.firstInstanceOfOrNull<ItemHolder>()
        itemConfigMenu = if (itemHolder != null && inventories != null)
            ItemSideConfigMenu(endPoint, itemHolder, inventories)
        else null
        
        val fluidHolder = endPoint.holders.firstInstanceOfOrNull<FluidHolder>()
        fluidConfigMenu = if (fluidHolder != null && containers != null)
            FluidSideConfigMenu(endPoint, fluidHolder, containers)
        else null
        
        require(energyConfigMenu != null || itemConfigMenu != null || fluidConfigMenu != null)
        
        mainGui = TabGui.normal()
            .setStructure(
                "< # # e i f # # #",
                "- - - - - - - - -",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x"
            )
            .addIngredient('<', BackItem(openPrevious = openPrevious))
            .addIngredient('e', ClickyTabItem(0) {
                (if (energyConfigMenu != null) {
                    if (it.currentTab == 0)
                        DefaultGuiItems.ENERGY_BTN_SELECTED
                    else DefaultGuiItems.ENERGY_BTN_ON
                } else DefaultGuiItems.ENERGY_BTN_OFF).model.clientsideProvider
            })
            .addIngredient('i', ClickyTabItem(1) {
                (if (itemConfigMenu != null) {
                    if (it.currentTab == 1)
                        DefaultGuiItems.ITEM_BTN_SELECTED
                    else DefaultGuiItems.ITEM_BTN_ON
                } else DefaultGuiItems.ITEM_BTN_OFF).model.clientsideProvider
            })
            .addIngredient('f', ClickyTabItem(2) {
                (if (fluidConfigMenu != null) {
                    if (it.currentTab == 2)
                        DefaultGuiItems.FLUID_BTN_SELECTED
                    else DefaultGuiItems.FLUID_BTN_ON
                } else DefaultGuiItems.FLUID_BTN_OFF).model.clientsideProvider
            })
            .setTabs(listOf(energyConfigMenu?.gui, itemConfigMenu?.gui, fluidConfigMenu?.gui))
            .build()
        
        updateNetworkData()
    }
    
    /**
     * Opens a [Window] of this [SideConfigMenu] for the given [player].
     */
    fun openWindow(player: Player) {
        Window.single {
            it.setViewer(player)
            it.setTitle(Component.translatable("menu.nova.side_config"))
            it.setGui(mainGui)
            it.addOpenHandler(::updateNetworkData)
        }.open()
    }
    
    private fun updateNetworkData() {
        NetworkManager.queueRead(endPoint.pos.world) {
            energyConfigMenu?.initAsync()
            itemConfigMenu?.initAsync()
            fluidConfigMenu?.initAsync()
        }
    }
    
}

/**
 * An ui item that opens the [sideConfigMenu] when clicked.
 */
class OpenSideConfigItem(private val sideConfigMenu: SideConfigMenu) : SimpleItem(DefaultGuiItems.SIDE_CONFIG_BTN.model.clientsideProvider) {
    
    override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
        player.playClickSound()
        sideConfigMenu.openWindow(player)
    }
    
}