package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.recipe.PulverizerRecipe
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenSideConfigItem
import xyz.xenondevs.nova.ui.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.ui.item.PulverizerProgress
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.seed
import java.util.*

private const val MAX_ENERGY = 10_000
private const val ENERGY_PER_TICK = 50
private const val PULVERIZE_TIME = 200

class Pulverizer(material: NovaMaterial, armorStand: ArmorStand) : TileEntity(material, armorStand), EnergyStorage {
    
    private var energy = retrieveData(0, "energy")
    private val inputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("input"), 1).apply { setItemUpdateHandler(::handleInputUpdate) }
    private val outputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("output"), 2).apply { setItemUpdateHandler(::handleOutputUpdate) }
    private val gui = PulverizerGUI()
    private var updateEnergyBar = true
    
    private var pulverizeTime: Int = 0
    private var currentItem: ItemStack? = null
    
    override val networks = EnumMap<NetworkType, MutableMap<BlockFace, Network>>(NetworkType::class.java)
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> = retrieveData(createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT), "sideConfig")
    override val allowedFaces: Map<NetworkType, List<BlockFace>>
        get() = mapOf(NetworkType.ENERGY to energyConfig.filterNot { it.value == EnergyConnectionType.NONE }.map { it.key })
    override val providedEnergy = 0
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            if (pulverizeTime == 0) takeItem()
            else {
                pulverizeTime--
                energy -= ENERGY_PER_TICK
                
                if (pulverizeTime == 0) {
                    outputInv.place(null, 0, currentItem)
                }
                
                gui.updateProgress()
                updateEnergyBar = true
            }
        }
        
        if (updateEnergyBar) {
            gui.energyBar.update()
            updateEnergyBar = false
        }
    }
    
    private fun takeItem() {
        val inputItem = inputInv.getItemStack(0)
        if (inputItem != null) {
            val outputItem = outputInv.getItemStack(0)
            val recipeOutput = PulverizerRecipe.getOutputFor(inputItem.type)
            if (outputItem == null || outputItem.type == recipeOutput.type) {
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
    
    override fun addEnergy(energy: Int) {
        this.energy += energy
        updateEnergyBar = true
    }
    
    override fun removeEnergy(energy: Int) {
        throw UnsupportedOperationException()
    }
    
    override fun saveData() {
        storeData("energy", energy, true)
        storeData("sideConfig", energyConfig)
    }
    
    override fun handleInitialized() {
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
    private fun getEnergyValues() = energy to MAX_ENERGY
    
    inner class PulverizerGUI {
        
        private val mainProgress = ProgressArrowItem()
        private val pulverizerProgress = PulverizerProgress()
        private val sideConfigGUI = SideConfigGUI(this@Pulverizer, EnergyConnectionType.NONE, EnergyConnectionType.CONSUME) { openWindow(it) }
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
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, ::getEnergyValues)
        
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