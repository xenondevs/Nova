package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.PROVIDE
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.EnergyProgressItem
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.util.EnergyUtils
import xyz.xenondevs.nova.util.fuel
import xyz.xenondevs.nova.util.runAsyncTaskLater
import xyz.xenondevs.nova.util.toItemStack
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig.getInt("furnace_generator.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("furnace_generator.energy_per_tick")!!
private val BURN_TIME_MULTIPLIER = NovaConfig.getDouble("furnace_generator.burn_time_multiplier")!!

class FurnaceGenerator(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(PROVIDE, FRONT) }
    
    private var burnTime: Int = retrieveData("burnTime") { 0 }
    private var totalBurnTime: Int = retrieveData("totalBurnTime") { 0 }
    
    private val inventory = getInventory("fuel", 1, true, ::handleInventoryUpdate)
    
    private val gui by lazy { CoalGeneratorGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    override fun handleTick() {
        if (burnTime == 0) burnItem()
        
        if (burnTime != 0) {
            burnTime--
            energy = min(MAX_ENERGY, energy + ENERGY_PER_TICK)
            
            gui.progressItem.percentage = burnTime.toDouble() / totalBurnTime.toDouble()
            hasEnergyChanged = true
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    private fun burnItem() {
        val fuelStack = inventory.getItemStack(0)
        if (energy < MAX_ENERGY && fuelStack != null) {
            val fuel = fuelStack.type.fuel
            if (fuel != null) {
                burnTime += (fuel.burnTime * BURN_TIME_MULTIPLIER).roundToInt()
                totalBurnTime = burnTime
                if (fuel.remains == null) {
                    inventory.removeOne(null, 0)
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
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        
        runAsyncTaskLater(1) {
            when (event.hand) {
                EquipmentSlot.HAND -> event.player.swingMainHand()
                EquipmentSlot.OFF_HAND -> event.player.swingOffHand()
                else -> Unit
            }
        }
        
        gui.openWindow(event.player)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("burnTime", burnTime)
        storeData("totalBurnTime", totalBurnTime)
    }
    
    inner class CoalGeneratorGUI {
        
        val progressItem = EnergyProgressItem()
        
        private val sideConfigGUI = SideConfigGUI(
            this@FurnaceGenerator,
            listOf(NONE, PROVIDE),
            listOf(inventory to "Fuel Inventory")
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # # |" +
                "| # # # i # # # |" +
                "| # # # ! # # # |" +
                "| # # # # # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inventory, 0))
            .addIngredient('!', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4) { Triple(energy, MAX_ENERGY, -1) }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Furnace Generator", gui).show()
        }
    }
    
}
