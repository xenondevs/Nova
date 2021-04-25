package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.recipe.PulverizerRecipe
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.ui.item.PulverizerProgress
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.seed

private const val MAX_ENERGY = 10_000
private const val ENERGY_PER_TICK = 50
private const val PULVERIZE_TIME = 200

// TODO: Make PULVERIZE_TIME recipe dependent
class Pulverizer(
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(material, armorStand) {
    
    private val inputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("input"), 1).apply { setItemUpdateHandler(::handleInputUpdate) }
    private val outputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("output"), 2).apply { setItemUpdateHandler(::handleOutputUpdate) }
    private val gui = PulverizerGUI()
    
    private var pulverizeTime: Int = 0
    private var currentItem: ItemStack? = null
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    init {
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
            val recipeOutput = PulverizerRecipe.getOutputFor(inputItem.type)
            if (outputInv.simulateAdd(recipeOutput) == 0) {
                inputInv.removeOne(null, 0)
                currentItem = recipeOutput
                pulverizeTime = PULVERIZE_TIME
            }
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    private fun handleInputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null && event.newItemStack != null) {
            val material = event.newItemStack.type
            if (!PulverizerRecipe.isPulverizable(material)) {
                event.isCancelled = true
            }
        }
    }
    
    private fun handleOutputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null && event.newItemStack != null) event.isCancelled = true
    }
    
    inner class PulverizerGUI {
        
        private val mainProgress = ProgressArrowItem()
        private val pulverizerProgress = PulverizerProgress()
        
        private val sideConfigGUI = SideConfigGUI(
            this@Pulverizer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(inputInv to "Input Inventory", outputInv to "Output Inventory"),
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
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
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { energy to MAX_ENERGY }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Pulverizer", gui).show()
        }
        
        fun updateProgress() {
            val percentage = if (pulverizeTime == 0) 0.0 else (PULVERIZE_TIME - pulverizeTime).toDouble() / PULVERIZE_TIME.toDouble()
            mainProgress.percentage = percentage
            pulverizerProgress.percentage = percentage
        }
        
    }
    
}