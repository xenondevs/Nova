package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.HARVESTER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType.BUFFER
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType.EXTRACT
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.addAll
import xyz.xenondevs.nova.util.breakAndTakeDrops
import xyz.xenondevs.nova.util.dropItemsNaturally
import xyz.xenondevs.nova.util.item.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val MAX_ENERGY = NovaConfig[HARVESTER].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[HARVESTER].getLong("energy_per_tick")!!
private val ENERGY_PER_BREAK = NovaConfig[HARVESTER].getLong("energy_per_break")!!
private val IDLE_TIME = NovaConfig[HARVESTER].getInt("idle_time")!!
private val MIN_RANGE = NovaConfig[HARVESTER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[HARVESTER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[HARVESTER].getInt("range.default")!!

class Harvester(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("harvest", 12, ::handleInventoryUpdate)
    private val shearInventory = getInventory("shears", 1, ::handleShearInventoryUpdate)
    private val axeInventory = getInventory("axe", 1, ::handleAxeInventoryUpdate)
    private val hoeInventory = getInventory("hoe", 1, ::handleHoeInventoryUpdate)
    override val gui = lazy(::HarvesterGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_BREAK, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to EXTRACT, shearInventory to BUFFER, axeInventory to BUFFER, hoeInventory to BUFFER)
    
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var harvestRegion: Region
    
    private val queuedBlocks = LinkedList<Pair<Block, Material>>()
    private var timePassed = 0
    private var loadCooldown = 0
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        if (range > maxRange) range = maxRange
    }
    
    private fun updateRegion() {
        harvestRegion = getBlockFrontRegion(range, range, range * 2, 0)
        VisualRegion.updateRegion(uuid, harvestRegion)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                loadCooldown--
                
                if (timePassed++ >= maxIdleTime) {
                    timePassed = 0
                    
                    if (queuedBlocks.isEmpty()) loadBlocks()
                    harvestNextBlock()
                }
            }
        }
    }
    
    private fun loadBlocks() {
        if (loadCooldown <= 0) {
            loadCooldown = 100
            
            queuedBlocks += harvestRegion
                .blocks
                .filter { it.isHarvestable() }
                .sortedWith(HarvestPriorityComparator)
                .map { it to it.type }
        }
    }
    
    private fun harvestNextBlock() {
        do {
            var tryAgain = false
            
            if (queuedBlocks.isNotEmpty()) {
                // get next block
                val (block, expectedType) = queuedBlocks.first
                queuedBlocks.removeFirst()
                
                if (!ProtectionManager.canBreak(ownerUUID, block.location)) {
                    // skip block if it is protected
                    tryAgain = true
                    continue
                }
                
                // check that the type hasn't changed
                if (block.type == expectedType) {
                    var tool: ItemStack? = null
                    if (Tag.MINEABLE_AXE.isTagged(expectedType)) {
                        // set tool to axe
                        tool = axeInventory.getItemStack(0)
                        
                        if (!useTool(axeInventory)) {
                            // skip block if axe is not available
                            tryAgain = true
                            continue
                        }
                    } else if (Tag.LEAVES.isTagged(expectedType)) {
                        // set tool to shears
                        tool = shearInventory.getItemStack(0)
                        useTool(shearInventory)
                    } else if (Tag.MINEABLE_HOE.isTagged(expectedType)) {
                        // set tool to hoe
                        tool = hoeInventory.getItemStack(0)
                        
                        if (!useTool(hoeInventory)) {
                            // skip block if hoe is not available
                            tryAgain = true
                            continue
                        }
                    }
                    
                    val drops = if (PlantUtils.COMPLEX_HARVESTABLE_BLOCKS.contains(expectedType)) {
                        // use complex harvesting method to harvest this block
                        listOf(PlantUtils.COMPLEX_HARVESTABLE_BLOCKS[expectedType]!!.second(block))
                    } else {
                        // break the drops with the provided tool
                        block.breakAndTakeDrops(tool)
                    }
                    
                    // add the drops to the inventory or drop them in the world if they don't fit
                    if (inventory.canHold(drops))
                        inventory.addAll(SELF_UPDATE_REASON, drops)
                    else world.dropItemsNaturally(block.location, drops)
                    
                    // take energy
                    energyHolder.energy -= energyHolder.specialEnergyConsumption
                } else tryAgain = true
            }
            
        } while (tryAgain)
    }
    
    private fun useTool(inventory: VirtualInventory): Boolean {
        val itemStack = inventory.getItemStack(0)
        if (itemStack != null) {
            inventory.setItemStack(SELF_UPDATE_REASON, 0, ToolUtils.damageTool(itemStack))
            return true
        }
        
        return false
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && event.isAdd
    }
    
    private fun handleShearInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && event.newItemStack.type != Material.SHEARS
    }
    
    private fun handleAxeInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && !event.newItemStack.type.isAxe()
    }
    
    private fun handleHoeInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.newItemStack != null && !event.newItemStack.type.isHoe()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class HarvesterGUI : TileEntityGUI("menu.nova.harvester") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Harvester,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output",
                itemHolder.getNetworkedInventory(shearInventory) to "inventory.nova.shears",
                itemHolder.getNetworkedInventory(axeInventory) to "inventory.nova.axes",
                itemHolder.getNetworkedInventory(hoeInventory) to "inventory.nova.hoes",
            )
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| c v u s a h . |" +
                "| m n p # # # . |" +
                "| i i i i i i . |" +
                "| i i i i i i . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('c', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('v', VisualizeRegionItem(uuid) { harvestRegion })
            .addIngredient('s', VISlotElement(shearInventory, 0, NovaMaterialRegistry.SHEARS_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('a', VISlotElement(axeInventory, 0, NovaMaterialRegistry.AXE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('h', VISlotElement(hoeInventory, 0, NovaMaterialRegistry.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, 7, 1, 4, energyHolder)
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}

private object HarvestPriorityComparator : Comparator<Block> {
    
    @Suppress("LiftReturnOrAssignment")
    override fun compare(o1: Block, o2: Block): Int {
        val type1 = o1.type
        val type2 = o2.type
        
        fun compareLocation() = o2.location.y.compareTo(o1.location.y)
        
        if (type1 == type2) compareLocation()
        
        if (type1.isTreeAttachment()) {
            if (type2.isTreeAttachment()) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (type2.isTreeAttachment()) {
            return 1
        }
        
        if (type1.isLeaveLike()) {
            if (type2.isLeaveLike()) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (type2.isLeaveLike()) {
            return 1
        }
        
        if (Tag.LOGS.isTagged(type1)) {
            if (Tag.LOGS.isTagged(type2)) {
                return compareLocation()
            } else {
                return -1
            }
        } else if (Tag.LOGS.isTagged(type2)) {
            return 1
        }
        
        return compareLocation()
    }
    
}