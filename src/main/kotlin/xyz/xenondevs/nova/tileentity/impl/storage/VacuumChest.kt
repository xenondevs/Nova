package xyz.xenondevs.nova.tileentity.impl.storage

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.entity.Item
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.item.impl.getFilterConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.ItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.tileentity.upgrade.Upgradeable
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.dropItems
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import de.studiocode.invui.item.Item as UIItem

private val MIN_RANGE = NovaConfig.getInt("vacuum_chest.range.min")!!
private val MAX_RANGE = NovaConfig.getInt("vacuum_chest.range.max")!!

class VacuumChest(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : ItemTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradeable {
    
    override val upgradeHolder = UpgradeHolder(this, data, UpgradeType.RANGE) { gui }
    private val inventory: VirtualInventory
    override val gui by lazy { VacuumChestGUI() }
    private val filterInventory = getInventory("itemFilter", 1, true, intArrayOf(1), ::handleFilterInventoryUpdate)
    private var filter: ItemFilter? = filterInventory.getItemStack(0)?.getFilterConfig()
    private val items = ArrayList<Item>()
    
    private lateinit var region: Region
    private var range = retrieveData("range") { MAX_RANGE }
        set(value) {
            field = value
            updateRegion()
            gui.updateRangeItems()
        }
    private val maxRange: Int
        get() = MAX_RANGE + upgradeHolder.getRangeModifier().toInt()
    
    private var tick = 0
    
    init {
        updateRegion()
        
        // -- Start legacy support --
        // this drops all items of the previously 12 slot inventory and then deletes the inventory
        val inventoryUUID = uuid.salt("inventory")
        val legacyInventory = VirtualInventoryManager.getInstance().getByUuid(inventoryUUID)
        if (legacyInventory != null && legacyInventory.size != 9) {
            location.dropItems(legacyInventory.items.filterNotNull())
            VirtualInventoryManager.getInstance().remove(legacyInventory)
        }
        // -- End legacy support --
        
        inventory = getInventory("inventory", 9, true) {}
        setDefaultInventory(inventory)
    }
    
    override fun saveData() {
        storeData("range", range)
        super.saveData()
    }
    
    private fun updateRegion() {
        region = Region(
            location.clone().center().subtract(range.toDouble(), range.toDouble(), range.toDouble()),
            location.clone().center().add(range.toDouble(), range.toDouble(), range.toDouble())
        )
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun handleUpgradeUpdates() {
        if (range > maxRange) {
            range = maxRange // the setter will update everything else
        } else gui.updateRangeItems()
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
        if (newStack?.novaMaterial == NovaMaterialRegistry.ITEM_FILTER)
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
        
        private val rangeItems = ArrayList<UIItem>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # . . . p |" +
                "| r # # . . . d |" +
                "| f # # . . . m |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('r', VisualizeRegionItem(uuid) { region })
            .addIngredient('f', VISlotElement(filterInventory, 0, NovaMaterialRegistry.ITEM_FILTER_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('d', DisplayNumberItem { range }.also(rangeItems::add))
            .build()
            .also { it.fillRectangle(4, 1, 3, inventory, true) }
        
        fun updateRangeItems() {
            rangeItems.forEach(UIItem::notifyWindows)
        }
        
    }
    
}