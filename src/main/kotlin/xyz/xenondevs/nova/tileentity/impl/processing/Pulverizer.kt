package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.ui.item.PulverizerProgress
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.particle
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("pulverizer.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("pulverizer.energy_per_tick")!!
private val PULVERIZE_TIME = NovaConfig.getInt("pulverizer.pulverizer_time")!!

// TODO: Make PULVERIZE_TIME recipe dependent
class Pulverizer(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    private val inputInv = getInventory("input", 1, true, ::handleInputUpdate)
    private val outputInv = getInventory("output", 2, true, ::handleOutputUpdate)
    private var pulverizeTime = retrieveData("pulverizerTime") { 0 }
    
    private var currentItem: ItemStack? = retrieveOrNull("currentItem")
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    override val gui by lazy { PulverizerGUI() }
    
    private val particleTask = createParticleTask(listOf(
        particle(ParticleEffect.SMOKE_NORMAL) {
            location(armorStand.location.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.8 })
            offset(0.05, 0.2, 0.05)
            speed(0f)
        }
    ), 6)
    
    init {
        addAvailableInventories(inputInv, outputInv)
        setDefaultInventory(inputInv)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            if (pulverizeTime == 0) {
                takeItem()
                
                if (particleTask.isRunning()) particleTask.stop()
            } else {
                pulverizeTime--
                energy -= ENERGY_PER_TICK
                
                if (!particleTask.isRunning()) particleTask.start()
                
                if (pulverizeTime == 0) {
                    outputInv.addItem(null, currentItem)
                    currentItem = null
                }
                
                gui.updateProgress()
            }
            
        } else if (particleTask.isRunning()) particleTask.stop()
        
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
                "| s u # # # # . |" +
                "| i # , # o a . |" +
                "| c # # # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInv, 0))
            .addIngredient('o', VISlotElement(outputInv, 0))
            .addIngredient('a', VISlotElement(outputInv, 1))
            .addIngredient(',', mainProgress)
            .addIngredient('c', pulverizerProgress)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', UpgradesTeaserItem)
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