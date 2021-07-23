package xyz.xenondevs.nova.tileentity.impl.processing

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.recipe.RecipeManager
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.ui.item.PulverizerProgress
import xyz.xenondevs.nova.util.BlockSide
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("pulverizer.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("pulverizer.energy_per_tick")!!
private val PULVERIZE_TIME = NovaConfig.getInt("pulverizer.pulverizer_time")!!

// TODO: Make PULVERIZE_TIME recipe dependent
class Pulverizer(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, data, armorStand) {
    
    private val inputInv = getInventory("input", 1, true, ::handleInputUpdate)
    private val outputInv = getInventory("output", 2, true, ::handleOutputUpdate)
    private var pulverizeTime = retrieveData("pulverizerTime") { 0 }
    
    private var currentItem: ItemStack? = retrieveOrNull("currentItem")
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    override val gui by lazy { PulverizerGUI() }
    
    init {
        addAvailableInventories(inputInv, outputInv)
        setDefaultInventory(inputInv)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            if (pulverizeTime == 0) takeItem()
            else {
                pulverizeTime--
                energy -= ENERGY_PER_TICK
                
                if (pulverizeTime == 0) {
                    outputInv.addItem(null, currentItem)
                    currentItem = null
                }
                
                gui.updateProgress()
            }
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    private fun takeItem() {
        val inputItem = inputInv.getItemStack(0)
        if (inputItem != null) {
            val recipeOutput = RecipeManager.getPulverizerOutputFor(inputItem)!!
            if (outputInv.simulateAdd(recipeOutput)[0] == 0) {
                inputInv.addItemAmount(null, 0, -1)
                currentItem = recipeOutput
                pulverizeTime = PULVERIZE_TIME
            }
        }
    }
    
    private fun handleInputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null
            && event.newItemStack != null
            && RecipeManager.getPulverizerOutputFor(event.newItemStack) == null) {
            
            event.isCancelled = true
        }
    }
    
    private fun handleOutputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null && event.newItemStack != null) event.isCancelled = true
    }
    
    override fun saveData() {
        super.saveData()
        storeData("pulverizerTime", pulverizeTime)
        storeData("currentItem", currentItem)
    }
    
    inner class PulverizerGUI : TileEntityGUI("menu.nova.pulverizer") {
        
        private val mainProgress = ProgressArrowItem()
        private val pulverizerProgress = PulverizerProgress()
        
        private val sideConfigGUI = SideConfigGUI(
            this@Pulverizer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                Triple(getNetworkedInventory(inputInv), "inventory.nova.input", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(outputInv), "inventory.nova.output", ItemConnectionType.EXTRACT_TYPES)
            ),
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # . |" +
                "| i # , # o u . |" +
                "| c # # # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInv, 0))
            .addIngredient('o', VISlotElement(outputInv, 0))
            .addIngredient('u', VISlotElement(outputInv, 1))
            .addIngredient(',', mainProgress)
            .addIngredient('c', pulverizerProgress)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, if (currentItem != null) -ENERGY_PER_TICK else 0) }
        
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            val percentage = if (pulverizeTime == 0) 0.0 else (PULVERIZE_TIME - pulverizeTime).toDouble() / PULVERIZE_TIME.toDouble()
            mainProgress.percentage = percentage
            pulverizerProgress.percentage = percentage
        }
        
    }
    
}