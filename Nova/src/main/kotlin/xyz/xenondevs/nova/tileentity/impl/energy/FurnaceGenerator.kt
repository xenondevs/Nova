package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FURNACE_GENERATOR
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.PROVIDE
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.EnergyProgressItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.util.item.fuel
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig[FURNACE_GENERATOR].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[FURNACE_GENERATOR].getLong("energy_per_tick")!!
private val BURN_TIME_MULTIPLIER = NovaConfig[FURNACE_GENERATOR].getDouble("burn_time_multiplier")!!
private val ACCEPTED_UPGRADE_TYPES = arrayOf(UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)

class FurnaceGenerator(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { FurnaceGeneratorGUI() }
    private val inventory = getInventory("fuel", 1, ::handleInventoryUpdate)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = ACCEPTED_UPGRADE_TYPES)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder) { createEnergySideConfig(PROVIDE, FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.BUFFER)
    
    private var burnTimeMultiplier = BURN_TIME_MULTIPLIER
    private var burnTime: Int = retrieveData("burnTime") { 0 }
    private var totalBurnTime: Int = retrieveData("totalBurnTime") { 0 }
    private var active = burnTime != 0
        set(value) {
            if (field != value) {
                field = value
                if (value) particleTask.start()
                else particleTask.stop()
                
                updateHeadStack()
            }
        }
    
    private val particleTask = createParticleTask(listOf(
        particle(ParticleEffect.SMOKE_NORMAL) {
            location(armorStand.location.advance(getFace(FRONT), 0.6).apply { y += 0.8 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
            speed(0f)
            amount(5)
        }
    ), 1)
    
    init {
        if (active) particleTask.start()
        handleUpgradeUpdates()
    }
    
    private fun handleUpgradeUpdates() {
        // percent of the burn time left
        val burnPercentage = burnTime.toDouble() / totalBurnTime.toDouble()
        // previous burn time without the burnTimeMultiplier
        val previousBurnTime = totalBurnTime.toDouble() / burnTimeMultiplier
        // calculate the new burn time multiplier based on upgrades
        burnTimeMultiplier = BURN_TIME_MULTIPLIER / upgradeHolder.getSpeedModifier() * upgradeHolder.getEfficiencyModifier()
        // set the new total burn time based on the fuel burn time and the new multiplier
        totalBurnTime = (previousBurnTime * burnTimeMultiplier).toInt()
        // set the burn time based on the calculated total burn time and the percentage of burn time that was left previously
        burnTime = (totalBurnTime * burnPercentage).toInt()
    }
    
    override fun getHeadStack() =
        material.block!!.createItemStack(active.intValue)
    
    override fun handleTick() {
        if (burnTime == 0) burnItem()
        
        if (burnTime != 0) {
            burnTime--
            energyHolder.energy = min(energyHolder.maxEnergy, energyHolder.energy + energyHolder.energyGeneration)
            
            if (gui.isInitialized())
                gui.value.progressItem.percentage = burnTime.toDouble() / totalBurnTime.toDouble()
            
            if (!active) active = true
        } else if (active) active = false
    }
    
    private fun burnItem() {
        val fuelStack = inventory.getItemStack(0)
        if (energyHolder.energy < energyHolder.maxEnergy && fuelStack != null) {
            val fuel = fuelStack.type.fuel
            if (fuel != null) {
                burnTime += (fuel.burnTime * burnTimeMultiplier).roundToInt()
                totalBurnTime = burnTime
                if (fuel.remains == null) {
                    inventory.addItemAmount(null, 0, -1)
                } else {
                    inventory.setItemStack(null, 0, fuel.remains.toItemStack())
                }
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null) { // not done by the tileEntity itself
            if (event.newItemStack != null && event.newItemStack.type.fuel == null) {
                // illegal item
                event.isCancelled = true
            }
        }
    }
    
    override fun saveData() {
        super.saveData()
        storeData("burnTime", burnTime)
        storeData("totalBurnTime", totalBurnTime)
    }
    
    inner class FurnaceGeneratorGUI : TileEntityGUI() {
        
        val progressItem = EnergyProgressItem()
        
        private val sideConfigGUI = SideConfigGUI(
            this@FurnaceGenerator,
            listOf(NONE, PROVIDE),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.fuel")
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # # |" +
                "| u # # i # # # |" +
                "| # # # ! # # # |" +
                "| # # # # # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inventory, 0))
            .addIngredient('!', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4, energyHolder)
        
    }
    
}
