package xyz.xenondevs.nova.ui.menu.sideconfig

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.menu.item.tabItem
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.EnergyHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.clientsideProvider

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
        
        mainGui = tabGui(
            "< # # e i f # # #",
            "- - - - - - - - -",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
        ) {
            tabs by listOf(energyConfigMenu?.gui, itemConfigMenu?.gui, fluidConfigMenu?.gui)
            
            '<' by BackItem(openPrevious = openPrevious)
            'e' by tabItem(
                0, tab, tabs,
                DefaultGuiItems.ENERGY_BTN_SELECTED.clientsideProvider,
                DefaultGuiItems.ENERGY_BTN_ON.clientsideProvider,
                DefaultGuiItems.ENERGY_BTN_OFF.clientsideProvider
            )
            'i' by tabItem(
                1, tab, tabs,
                DefaultGuiItems.ITEM_BTN_SELECTED.clientsideProvider,
                DefaultGuiItems.ITEM_BTN_ON.clientsideProvider,
                DefaultGuiItems.ITEM_BTN_OFF.clientsideProvider
            )
            'f' by tabItem(
                2, tab, tabs,
                DefaultGuiItems.FLUID_BTN_SELECTED.clientsideProvider,
                DefaultGuiItems.FLUID_BTN_ON.clientsideProvider,
                DefaultGuiItems.FLUID_BTN_OFF.clientsideProvider
            )
        }
        
        updateNetworkData()
    }
    
    /**
     * Opens a [Window] of this [SideConfigMenu] for the given [player].
     */
    fun openWindow(player: Player) {
        val window = Window.builder()
            .setViewer(player)
            .setTitle(Component.translatable("menu.nova.side_config"))
            .setUpperGui(mainGui)
            .addOpenHandler(::updateNetworkData)
            .build()
        
        if (endPoint is TileEntity) {
            endPoint.menuContainer.registerWindow(window)
        }
        
        window.open()
    }
    
    private fun updateNetworkData() {
        NetworkManager.queueRead(endPoint.pos.chunkPos) {
            energyConfigMenu?.initAsync()
            itemConfigMenu?.initAsync()
            fluidConfigMenu?.initAsync()
        }
    }
    
}

/**
 * A UI item that opens the [sideConfigMenu] when clicked.
 */
fun openSideConfigItem(sideConfigMenu: SideConfigMenu): Item = item {
    itemProvider by DefaultGuiItems.SIDE_CONFIG_BTN.clientsideProvider
    onClick {
        player.playClickSound()
        sideConfigMenu.openWindow(player)
    }
}

@Suppress("FunctionName")
@Deprecated("", ReplaceWith("openSideConfigItem(sideConfigMenu)"))
fun OpenSideConfigItem(sideConfigMenu: SideConfigMenu) = openSideConfigItem(sideConfigMenu)