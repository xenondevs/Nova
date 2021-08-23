package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
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
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.addAll
import xyz.xenondevs.nova.util.breakAndTakeDrops
import xyz.xenondevs.nova.util.dropItemsNaturally
import xyz.xenondevs.nova.util.item.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("harvester.capacity")!!
private val ENERGY_PER_BREAK = NovaConfig.getInt("harvester.energy_per_break")!!
private val WAIT_TIME = NovaConfig.getInt("harvester.wait_time")!!

class Harvester(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val gui by lazy(::HarvesterGUI)
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("harvest", 12, true, ::handleInventoryUpdate)
    private val shearInventory = getInventory("shears", 1, true, ::handleShearInventoryUpdate)
    private val axeInventory = getInventory("axe", 1, true, ::handleAxeInventoryUpdate)
    private val hoeInventory = getInventory("hoe", 1, true, ::handleHoeInventoryUpdate)
    private val harvestRegion = getFrontArea(11.0, 11.0, 11.0, 0.0)
    
    private val queuedBlocks = LinkedList<Pair<Block, Material>>()
    private var idleTime = 0
    private var loadCooldown = 0
    
    init {
        setDefaultInventory(inventory)
        addAvailableInventories(shearInventory, axeInventory, hoeInventory)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_BREAK) {
            loadCooldown--
            
            if (--idleTime <= 0) {
                idleTime = WAIT_TIME
                
                if (queuedBlocks.isEmpty()) loadBlocks()
                harvestNextBlock()
            }
            
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
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
                    energy -= ENERGY_PER_BREAK
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
                Triple(getNetworkedInventory(inventory), "inventory.nova.output", ItemConnectionType.EXTRACT_TYPES),
                Triple(getNetworkedInventory(shearInventory), "inventory.nova.shears", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(axeInventory), "inventory.nova.axes", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(hoeInventory), "inventory.nova.hoes", ItemConnectionType.ALL_TYPES)
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| c v u s a h . |" +
                "| # # # # # # . |" +
                "| . . . . . . . |" +
                "| . . . . . . . |" +
                "3 - - - - - - - 4")
            .addIngredient('c', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('v', VisualizeRegionItem(uuid, harvestRegion))
            .addIngredient('s', VISlotElement(shearInventory, 0, NovaMaterialRegistry.SHEARS_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('a', VISlotElement(axeInventory, 0, NovaMaterialRegistry.AXE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('h', VISlotElement(hoeInventory, 0, NovaMaterialRegistry.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('u', UpgradesTeaserItem)
            .build()
            .apply { fillRectangle(1, 3, 6, inventory, true) }
        
        val energyBar = EnergyBar(gui, 7, 1, 4) { Triple(energy, MAX_ENERGY, -1) }
        
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