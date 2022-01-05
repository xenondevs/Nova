package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
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
import xyz.xenondevs.nova.material.NovaMaterialRegistry.PLANTER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
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
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.item.*
import xyz.xenondevs.nova.util.soundGroup
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.random.Random

private val MAX_ENERGY = NovaConfig[PLANTER].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[PLANTER].getLong("energy_per_tick")!!
private val ENERGY_PER_PLANT = NovaConfig[PLANTER].getLong("energy_per_plant")!!
private val IDLE_TIME = NovaConfig[PLANTER].getInt("idle_time")!!
private val MIN_RANGE = NovaConfig[PLANTER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[PLANTER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[PLANTER].getInt("range.default")!!

class Planter(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inputInventory = getInventory("input", 6, ::handleSeedUpdate)
    private val hoesInventory = getInventory("hoes", 1, ::handleHoeUpdate)
    override val gui = lazy(::PlanterGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_PLANT, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inputInventory to NetworkConnectionType.BUFFER, hoesInventory to NetworkConnectionType.BUFFER)
    
    private var autoTill = retrieveData("autoTill") { true }
    private var maxIdleTime = 0
    private var timePassed = 0
    
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    private lateinit var plantRegion: Region
    private lateinit var soilRegion: Region
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        plantRegion = getBlockFrontRegion(range, range, 1, 0)
        soilRegion = Region(plantRegion.min.clone().advance(BlockFace.DOWN), plantRegion.max.clone().advance(BlockFace.DOWN))
        
        VisualRegion.updateRegion(uuid, plantRegion)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption // idle energy consumption
            
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption && timePassed++ >= maxIdleTime) {
                timePassed = 0
                placeNextSeed()
            }
        }
    }
    
    private fun placeNextSeed() {
        if (!inputInventory.isEmpty) {
            // loop over items until a placeable seed has been found
            for ((index, item) in inputInventory.items.withIndex()) {
                if (item == null) continue
                
                // find a location to place this seed or skip to the next one if there isn't one
                val (plant, soil) = getNextBlock(item.type) ?: continue
                energyHolder.energy -= energyHolder.specialEnergyConsumption
                
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
                energyHolder.energy -= energyHolder.specialEnergyConsumption
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
        storeData("range", range)
    }
    
    inner class PlanterGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Planter,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(hoesInventory) to "inventory.nova.hoes",
            )
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u v # # p e |" +
                "| i i i # h n e |" +
                "| i i i # f m e |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inputInventory)
            .addIngredient('h', VISlotElement(hoesInventory, 0, NovaMaterialRegistry.HOE_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('v', VisualizeRegionItem(uuid) { plantRegion })
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', AutoTillingItem())
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
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