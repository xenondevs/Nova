package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
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
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.item.*
import xyz.xenondevs.nova.util.soundGroup
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.random.Random

private val MAX_ENERGY = NovaConfig.getInt("planter.capacity")!!
private val ENERGY_PER_PLANT = NovaConfig.getInt("planter.energy_per_plant")!!
private val WAIT_TIME = NovaConfig.getInt("planter.wait_time")!!

class Planter(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inputInventory = getInventory("input", 6, true, ::handleSeedUpdate)
    private val hoesInventory = getInventory("hoes", 1, true, ::handleHoeUpdate)
    override val gui by lazy(::PlanterGUI)
    
    private val plantRegion = getFrontArea(7.0, 7.0, 1.0, 0.0)
    private val soilRegion = Region(plantRegion.min.clone().advance(BlockFace.DOWN), plantRegion.max.clone().advance(BlockFace.DOWN))
    
    private var autoTill = retrieveData("autoTill") { true }
    private var idleTime = WAIT_TIME
    
    init {
        addAvailableInventories(inputInventory, hoesInventory)
        setDefaultInventory(inputInventory)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_PLANT) {
            if (idleTime > 0) idleTime--
            else {
                idleTime = WAIT_TIME
                placeNextSeed()
            }
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    private fun placeNextSeed() {
        if (!inputInventory.isEmpty) {
            // loop over items until a placeable seed has been found
            for ((index, item) in inputInventory.items.withIndex()) {
                if (item == null) continue
                
                // find a location to place this seed or skip to the next one if there isn't one
                val (plant, soil) = getNextBlock(item.type) ?: continue
                energy -= ENERGY_PER_PLANT
                
                // till dirt if possible
                if (soil.type.isTillable() && autoTill && !hoesInventory.isEmpty) tillDirt(soil)
                
                // plant the seed
                plant.type = PlantUtils.SEED_BLOCKS[item.type] ?: item.type
                plant.world.playSound(plant.location, plant.type.soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
                
                // remove one from the seed stack
                inputInventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                // break the loop as a seed has been placed
                break
            }
        } else if (autoTill && !hoesInventory.isEmpty) {
            val block = getNextBlock(null)?.second
            if (block != null) {
                energy -= ENERGY_PER_PLANT
                tillDirt(block)
            }
        }
    }
    
    private fun getNextBlock(seedMaterial: Material?): Pair<Block, Block>? {
        val emptyHoes = hoesInventory.isEmpty
        val index = plantRegion.withIndex().indexOfFirst { (index, block) ->
            val soilBlock = soilRegion[index]
            val soilType = soilBlock.type
            
            // If there are no seeds search for dirt that can be tilled
            if (seedMaterial == null) return@indexOfFirst autoTill && !emptyHoes && soilType.isTillable()
            
            // Search for a block that has no block on top of it and is dirt/farmland
            // If the soil or plant block is protected, skip this block
            if (!ProtectionManager.canPlace(ownerUUID, block.location) || !ProtectionManager.canBreak(ownerUUID, soilBlock.location))
                return@indexOfFirst false
            
            // If the plant block is already occupied return false
            if (!block.type.isAir)
                return@indexOfFirst false
            
            // If farmland is required and auto tilling is disabled or there are no hoes available,
            // only use this block if it's already farmland
            if (seedMaterial.requiresFarmland() && (!autoTill || emptyHoes))
                return@indexOfFirst soilType == Material.FARMLAND
            
            // if soil type is applicable for the seed or can be made applicable
            return@indexOfFirst seedMaterial.canBePlacedOn(soilType) || (seedMaterial.canBePlacedOn(Material.FARMLAND) && autoTill && !emptyHoes && soilType.isTillable())
        }
        
        if (index == -1)
            return null
        return plantRegion[index] to soilRegion[index]
    }
    
    private fun tillDirt(block: Block) {
        block.type = Material.FARMLAND
        world.playSound(block.location, Sound.ITEM_HOE_TILL, 1f, 1f)
        useHoe()
    }
    
    private fun handleHoeUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && !event.newItemStack.type.isHoe())
            event.isCancelled = true
    }
    
    private fun handleSeedUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && event.newItemStack.type !in PlantUtils.PLANTS)
            event.isCancelled = true
    }
    
    private fun useHoe() {
        hoesInventory.setItemStack(null, 0, ToolUtils.damageTool(hoesInventory.items[0]))
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("autoTill", autoTill)
    }
    
    inner class PlanterGUI : TileEntityGUI("menu.nova.planter") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Planter,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                Triple(getNetworkedInventory(inputInventory), "inventory.nova.input", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(hoesInventory), "inventory.nova.hoes", ItemConnectionType.INSERT_TYPES)
            ),
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u a # # # . |" +
                "| . . . # h # . |" +
                "| . . . # f # . |" +
                "3 - - - - - - - 4")
            .addIngredient('h', VISlotElement(hoesInventory, 0, NovaMaterialRegistry.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('a', VisualizeRegionItem(uuid) { plantRegion })
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', AutoTillingItem())
            .addIngredient('u', UpgradesTeaserItem)
            .build()
            .also { it.fillRectangle(1, 2, 3, inputInventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
        private inner class AutoTillingItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return (if (autoTill) NovaMaterialRegistry.HOE_ON_BUTTON else NovaMaterialRegistry.HOE_OFF_BUTTON)
                    .createBasicItemBuilder().setLocalizedName("menu.nova.planter.autotill")
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                autoTill = !autoTill
                notifyWindows()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
}