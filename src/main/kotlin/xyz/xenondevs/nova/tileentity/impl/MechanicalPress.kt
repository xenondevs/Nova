package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.CONSUME
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.recipe.RecipeManager
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.PressProgressItem
import xyz.xenondevs.nova.util.BlockSide.FRONT
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("mechanical_press.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("mechanical_press.energy_per_tick")!!
private val PRESS_TIME = NovaConfig.getInt("mechanical_press.press_time")!!

enum class PressType {
    
    PLATE,
    GEAR
    
}

class MechanicalPress(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(CONSUME, FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private var type: PressType = retrieveData("pressType") { PressType.PLATE }
    private var pressTime: Int = retrieveData("pressTime") { 0 }
    private var currentItem: ItemStack? = retrieveOrNull("currentItem")
    
    private val inputInv = getInventory("input", 1, true, ::handleInputUpdate)
    private val outputInv = getInventory("output", 1, true, ::handleOutputUpdate)
    
    override val gui by lazy { MechanicalPressGUI() }
    
    init {
        addAvailableInventories(inputInv, outputInv)
        setDefaultInventory(inputInv)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) { // has energy to do anything
            if (pressTime == 0) takeItem()
            if (pressTime != 0) { // is pressing
                pressTime--
                energy -= ENERGY_PER_TICK
                
                if (pressTime == 0) {
                    outputInv.putItemStack(null, 0, currentItem!!)
                    currentItem = null
                }
                
                gui.updateProgress()
                hasEnergyChanged = true
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
            val outputStack = RecipeManager.getPressOutputFor(inputItem, type)
            if (outputStack != null && outputInv.simulateAdd(outputStack)[0] == 0) {
                inputInv.addItemAmount(null, 0, -1)
                currentItem = outputStack
                pressTime = PRESS_TIME
            }
        }
    }
    
    private fun handleInputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null
            && event.newItemStack != null
            && RecipeManager.getPressOutputFor(event.newItemStack, type) == null) {
            
            event.isCancelled = true
        }
    }
    
    private fun handleOutputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null && event.newItemStack != null) event.isCancelled = true
    }
    
    override fun saveData() {
        super.saveData()
        storeData("pressType", type)
        storeData("pressTime", pressTime)
        storeData("currentItem", currentItem)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload) gui.close()
    }
    
    inner class MechanicalPressGUI : TileEntityGUI("Mechanical Press") {
        
        private val pressProgress = PressProgressItem()
        private val pressTypeItems = ArrayList<PressTypeItem>()
        
        private val sideConfigGUI = SideConfigGUI(
            this@MechanicalPress,
            listOf(NONE, CONSUME),
            listOf(
                Triple(getNetworkedInventory(inputInv), "Input Inventory", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(outputInv), "Output Inventory", ItemConnectionType.EXTRACT_TYPES),
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| p g # i # # . |" +
                "| # # # , # # . |" +
                "| s # # o # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInv, 0))
            .addIngredient('o', VISlotElement(outputInv, 0))
            .addIngredient(',', pressProgress)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('p', PressTypeItem(PressType.PLATE).apply(pressTypeItems::add))
            .addIngredient('g', PressTypeItem(PressType.GEAR).apply(pressTypeItems::add))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, if (currentItem != null) -ENERGY_PER_TICK else 0) }
        
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            pressProgress.percentage = if (pressTime == 0) 0.0 else (PRESS_TIME - pressTime).toDouble() / PRESS_TIME.toDouble()
        }
        
        fun close() = gui.closeForAllViewers()
        
        private inner class PressTypeItem(private val type: PressType) : BaseItem() {
            
            override fun getItemBuilder(): ItemBuilder {
                return if (type == PressType.PLATE) {
                    if (this@MechanicalPress.type == PressType.PLATE) NovaMaterial.PLATE_OFF_BUTTON.createItemBuilder()
                    else NovaMaterial.PLATE_ON_BUTTON.item.getItemBuilder("Press Plates")
                } else {
                    if (this@MechanicalPress.type == PressType.GEAR) NovaMaterial.GEAR_OFF_BUTTON.createItemBuilder()
                    else NovaMaterial.GEAR_ON_BUTTON.item.getItemBuilder("Press Gears")
                }
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                if (this@MechanicalPress.type != type) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    this@MechanicalPress.type = type
                    pressTypeItems.forEach(Item::notifyWindows)
                }
            }
            
        }
        
    }
    
}