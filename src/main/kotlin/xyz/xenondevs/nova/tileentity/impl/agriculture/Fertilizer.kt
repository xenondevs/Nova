package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FERTILIZER
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
import xyz.xenondevs.nova.util.blockPos
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.isFullyAged
import xyz.xenondevs.nova.util.nmsStack
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val MAX_ENERGY = NovaConfig[FERTILIZER].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[FERTILIZER].getLong("energy_per_tick")!!
private val ENERGY_PER_FERTILIZE = NovaConfig[FERTILIZER].getLong("energy_per_fertilize")!!
private val IDLE_TIME = NovaConfig[FERTILIZER].getInt("idle_time")!!
private val MIN_RANGE = NovaConfig[FERTILIZER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[FERTILIZER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[FERTILIZER].getInt("range.default")!!

class Fertilizer(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val fertilizerInventory = getInventory("fertilizer", 12, ::handleFertilizerUpdate)
    override val gui = lazy(::FertilizerGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_FERTILIZE, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, fertilizerInventory to NetworkConnectionType.BUFFER)
    
    private var maxIdleTime = 0
    private var timePassed = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var fertilizeRegion: Region
    
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
        fertilizeRegion = getBlockFrontRegion(range, range, 1, 0)
        VisualRegion.updateRegion(uuid, fertilizeRegion)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                if (timePassed++ >= maxIdleTime) {
                    timePassed = 0
                    if (!fertilizerInventory.isEmpty)
                        fertilizeNextPlant()
                }
            }
        }
    }
    
    private fun fertilizeNextPlant() {
        for ((index, item) in fertilizerInventory.items.withIndex()) {
            if (item == null) continue
            val plant = getRandomPlant() ?: return
            
            val context = UseOnContext(
                plant.world.serverLevel,
                null,
                InteractionHand.MAIN_HAND,
                item.nmsStack,
                BlockHitResult(Vec3.ZERO, Direction.DOWN, plant.location.blockPos, false)
            )
            BoneMealItem.applyBonemeal(context)
            
            energyHolder.energy -= energyHolder.specialEnergyConsumption
            fertilizerInventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
            break
        }
    }
    
    private fun getRandomPlant(): Block? =
        fertilizeRegion.blocks
            .filter {
                ProtectionManager.canUse(ownerUUID, it.location)
                    && ((it.blockData is Ageable && !it.isFullyAged()) || (it.blockData !is Ageable && it.type in PlantUtils.PLANTS))
            }
            .randomOrNull()
    
    private fun handleFertilizerUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && event.newItemStack.type != Material.BONE_MEAL)
            event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class FertilizerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Fertilizer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(fertilizerInventory) to "inventory.nova.fertilizer")
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s p i i i i . |" +
                "| v n i i i i . |" +
                "| u m i i i i . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', fertilizerInventory)
            .addIngredient('v', VisualizeRegionItem(uuid) { fertilizeRegion })
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}
