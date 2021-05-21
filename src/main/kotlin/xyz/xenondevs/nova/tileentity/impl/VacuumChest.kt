package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Item
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.impl.getFilterConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.ItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.isBetween
import xyz.xenondevs.nova.util.novaMaterial
import java.util.*

private val RANGE = NovaConfig.getDouble("vacuum_chest.range")!!

class VacuumChest(ownerUUID: UUID?, material: NovaMaterial, armorStand: ArmorStand) : ItemTileEntity(ownerUUID, material, armorStand) {
    
    private val inventory = getInventory("inventory", 12, true) {}
    private val filterInventory = getInventory("itemFilter", 1, true, ::handleFilterInventoryUpdate)
    
    private val pos1 = location.clone().center().subtract(RANGE, RANGE, RANGE)
    private val pos2 = location.clone().center().add(RANGE, RANGE, RANGE)
    
    private var items: List<Item>? = null
    
    override val gui by lazy { VacuumChestGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun handleTick() {
        items
            ?.filter { it.isValid }
            ?.forEach {
                val itemStack = it.itemStack
                val remaining = inventory.addItem(null, itemStack)
                if (remaining != 0) it.itemStack = itemStack.apply { amount = remaining }
                else it.remove()
            }
        
        val filter = getFilter()
        items = chunk.getSurroundingChunks(1, true, ignoreUnloaded = true)
            .flatMap { it.entities.asList() }
            .filterIsInstance<Item>()
            .filter { it.location.isBetween(pos1, pos2) }
            .filter { filter?.allowsItem(it.itemStack) ?: true }
            .onEach { it.velocity = location.clone().subtract(it.location).toVector() }
    }
    
    private fun handleFilterInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null && event.newItemStack?.novaMaterial != NovaMaterial.ITEM_FILTER)
            event.isCancelled = true
    }
    
    private fun getFilter() =
        filterInventory.getItemStack(0)?.getFilterConfig()
    
    inner class VacuumChestGUI : TileEntityGUI("Vacuum Chest") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@VacuumChest,
            null,
            listOf(
                Triple(getNetworkedInventory(inventory), "Inventory", ItemConnectionType.ALL_TYPES)
            ),
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . . . . # |" +
                "| r # . . . . # |" +
                "| f # . . . . # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid, pos1, pos2))
            .addIngredient('f', VISlotElement(filterInventory, 0))
            .build()
            .also { it.fillRectangle(3, 1, 4, inventory, true) }
        
    }
    
}