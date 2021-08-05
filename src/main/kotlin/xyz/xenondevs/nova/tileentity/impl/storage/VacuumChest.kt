package xyz.xenondevs.nova.tileentity.impl.storage

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.entity.Item
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.impl.getFilterConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemFilter
import xyz.xenondevs.nova.region.Region
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.ItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.novaMaterial
import java.util.*

private val RANGE = NovaConfig.getDouble("vacuum_chest.range")!!

class VacuumChest(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : ItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    private val inventory = getInventory("inventory", 12, true) {}
    private val filterInventory = getInventory("itemFilter", 1, true, intArrayOf(1), ::handleFilterInventoryUpdate)
    private var filter: ItemFilter? = filterInventory.getItemStack(0)?.getFilterConfig()
    private var tick = 0
    
    private val region = Region(
        location.clone().center().subtract(RANGE, RANGE, RANGE),
        location.clone().center().add(RANGE, RANGE, RANGE)
    )
    
    private val items = ArrayList<Item>()
    
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
            .forEach {
                if (it.isValid) {
                    val itemStack = it.itemStack
                    val remaining = inventory.addItem(null, itemStack)
                    if (remaining != 0) it.itemStack = itemStack.apply { amount = remaining }
                    else it.remove()
                }
            }
        
        items.clear()
        
        if (++tick == 10) {
            tick = 0
            world.entities.forEach {
                if (it is Item
                    && it.location in region
                    && filter?.allowsItem(it.itemStack) != false
                ) {
                    items += it
                    it.velocity = location.clone().subtract(it.location).toVector()
                }
            }
        }
    }
    
    private fun handleFilterInventoryUpdate(event: ItemUpdateEvent) {
        val newStack = event.newItemStack
        if (newStack?.novaMaterial == NovaMaterial.ITEM_FILTER)
            filter = newStack.getFilterConfig()
        else if (newStack != null) event.isCancelled = true
    }
    
    inner class VacuumChestGUI : TileEntityGUI("menu.nova.vacuum_chest") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@VacuumChest,
            null,
            listOf(
                Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES)
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
            .addIngredient('r', VisualizeRegionItem(uuid, region))
            .addIngredient('f', VISlotElement(filterInventory, 0, NovaMaterial.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
            .build()
            .also { it.fillRectangle(3, 1, 4, inventory, true) }
        
    }
    
}