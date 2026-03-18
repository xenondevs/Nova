package xyz.xenondevs.nova.ui.menu.sideconfig

import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.tabGui
import xyz.xenondevs.invui.dsl.window
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.ui.menu.by
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.menu.item.tabItem
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
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
    private val openPrevious: (Player) -> Unit
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
        
        initNetworkData()
    }
    
    /**
     * Opens a [Window] of this [SideConfigMenu] for the given [player].
     */
    fun openWindow(player: Player) {
        createWindow(player).open()
    }
    
    /**
     * Creates a [Window] for this [SideConfigMenu] for the given [player].
     * If the [endPoint] is a [TileEntity], the window is also registered to the [TileEntity's menu][TileEntity.menu].
     */
    fun createWindow(player: Player): Window {
        val window = window(player) {
            title by DefaultGuiTextures.SIDE_CONFIG
            upperGui by tabGui(
                "< x x x x x x x x",
                "e x x x x x x x x",
                "i x x x x x x x x",
                "f x x x x x x x x"
            ) {
                tabs by listOf(energyConfigMenu?.gui, itemConfigMenu?.gui, fluidConfigMenu?.gui)
                
                '<' by BackItem(DefaultGuiItems.TP_SMALL_ARROW_LEFT_ON.clientsideProvider, openPrevious)
                'e' by tabItem(
                    0, tab, tabs,
                    DefaultGuiItems.TP_ENERGY_BTN_SELECTED.clientsideProvider,
                    DefaultGuiItems.TP_ENERGY_BTN_ON.clientsideProvider,
                    DefaultGuiItems.TP_ENERGY_BTN_OFF.clientsideProvider
                )
                'i' by tabItem(
                    1, tab, tabs,
                    DefaultGuiItems.TP_ITEM_BTN_SELECTED.clientsideProvider,
                    DefaultGuiItems.TP_ITEM_BTN_ON.clientsideProvider,
                    DefaultGuiItems.TP_ITEM_BTN_OFF.clientsideProvider
                )
                'f' by tabItem(
                    2, tab, tabs,
                    DefaultGuiItems.TP_FLUID_BTN_SELECTED.clientsideProvider,
                    DefaultGuiItems.TP_FLUID_BTN_ON.clientsideProvider,
                    DefaultGuiItems.TP_FLUID_BTN_OFF.clientsideProvider
                )
            }
            onOpen { updateNetworkData() }
        }
        
        if (endPoint is TileEntity)
            endPoint.menu.register(window)
        
        return window
    }
    
    private fun initNetworkData() {
        NetworkManager.queueRead(endPoint.pos.chunkPos) { state ->
            energyConfigMenu?.init(state)
            itemConfigMenu?.init(state)
            fluidConfigMenu?.init(state)
        }        
    }
    
    internal fun updateNetworkData() {
        NetworkManager.queueRead(endPoint.pos.chunkPos) { state ->
            energyConfigMenu?.refresh(state)
            itemConfigMenu?.refresh(state)
            fluidConfigMenu?.refresh(state)
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

/**
 * A UI item that creates, memorizes, and opens a [SideConfigMenu] for the given [endPoint] and [containers] when clicked.
 * 
 * Uses the [window from the context][windowDsl] as the previous window.
 */
context(windowDsl: WindowDsl, endPoint: NetworkEndPoint)
fun openSideConfigItem(
    containers: Map<NetworkedFluidContainer, String>? = null
): Item = openSideConfigItem(null, containers)

/**
 * A UI item that creates, memorizes, and opens a [SideConfigMenu] for the given [endPoint], [inventories], and [containers] when clicked.
 * 
 * Uses the [window from the context][windowDsl] as the previous window.
 * 
 * Uses the [end point from the context][endPoint] to retrieve the [EndPointDataHolders][EndPointDataHolder].
 */
context(windowDsl: WindowDsl, endPoint: NetworkEndPoint)
fun openSideConfigItem(
    inventories: Map<NetworkedInventory, String>? = null,
    containers: Map<NetworkedFluidContainer, String>? = null,
    itemProvider: Provider<ItemProvider> = DefaultGuiItems.TP_SIDE_CONFIG_BTN.clientsideProvider
): Item = item {
    val outerWindow = windowDsl.window
    // eagerly create the menu as loading network state is async
    val menu = SideConfigMenu(endPoint, inventories, containers) { _ -> outerWindow.get().open() }
    val window = menu.createWindow(windowDsl.viewer)
    this.itemProvider by itemProvider
    onClick {
        player.playClickSound()
        window.open()
    }
    windowDsl.onOpen { menu.updateNetworkData() }
}

@Suppress("FunctionName")
@Deprecated("", ReplaceWith("openSideConfigItem(sideConfigMenu)"))
fun OpenSideConfigItem(sideConfigMenu: SideConfigMenu) = openSideConfigItem(sideConfigMenu)