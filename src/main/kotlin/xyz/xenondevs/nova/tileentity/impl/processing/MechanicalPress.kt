package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.CONSUME
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.PressProgressItem
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.max

private val MAX_ENERGY = NovaConfig.getInt("mechanical_press.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("mechanical_press.energy_per_tick")!!
private val PRESS_TIME = NovaConfig.getInt("mechanical_press.press_time")!!
private val PRESS_SPEED = NovaConfig.getInt("mechanical_press.speed")!!

enum class PressType {
    PLATE,
    GEAR
}

class MechanicalPress(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { MechanicalPressGUI() }
    
    private val inputInv = getInventory("input", 1, true, ::handleInputUpdate)
    private val outputInv = getInventory("output", 1, true, ::handleOutputUpdate)
    
    override val upgradeHolder = UpgradeHolder(data, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(CONSUME, FRONT) }
    override val itemHolder = NovaItemHolder(this, inputInv, outputInv)
    
    private var type: PressType = retrieveEnum("pressType") { PressType.PLATE }
    private var pressTime: Int = retrieveData("pressTime") { 0 }
    private var pressSpeed = 0
    
    private var currentItem: ItemStack? = retrieveOrNull("currentItem")
    
    init {
        handleUpgradeUpdates()
    }
    
    private fun handleUpgradeUpdates() {
        pressSpeed = (PRESS_SPEED * upgradeHolder.getSpeedModifier()).toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (pressTime == 0) takeItem()
            if (pressTime != 0) { // is pressing
                pressTime = max(pressTime - pressSpeed, 0)
                energyHolder.energy -= energyHolder.energyConsumption
                
                if (pressTime == 0) {
                    outputInv.putItemStack(null, 0, currentItem!!)
                    currentItem = null
                }
                
                if (gui.isInitialized()) gui.value.updateProgress()
            }
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
    
    
    inner class MechanicalPressGUI : TileEntityGUI("menu.nova.mechanical_press") {
        
        private val pressProgress = PressProgressItem()
        private val pressTypeItems = ArrayList<PressTypeItem>()
        
        private val sideConfigGUI = SideConfigGUI(
            this@MechanicalPress,
            listOf(NONE, CONSUME),
            listOf(
                Triple(itemHolder.getNetworkedInventory(inputInv), "inventory.nova.input", ItemConnectionType.ALL_TYPES),
                Triple(itemHolder.getNetworkedInventory(outputInv), "inventory.nova.output", ItemConnectionType.EXTRACT_TYPES),
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| p g # i # # . |" +
                "| # # # , # # . |" +
                "| s u # o # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInv, 0))
            .addIngredient('o', VISlotElement(outputInv, 0))
            .addIngredient(',', pressProgress)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('p', PressTypeItem(PressType.PLATE).apply(pressTypeItems::add))
            .addIngredient('g', PressTypeItem(PressType.GEAR).apply(pressTypeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            pressProgress.percentage = if (pressTime == 0) 0.0 else (PRESS_TIME - pressTime).toDouble() / PRESS_TIME.toDouble()
        }
        
        private inner class PressTypeItem(private val type: PressType) : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return if (type == PressType.PLATE) {
                    if (this@MechanicalPress.type == PressType.PLATE) NovaMaterialRegistry.PLATE_OFF_BUTTON.createItemBuilder()
                    else NovaMaterialRegistry.PLATE_ON_BUTTON.item.createItemBuilder("menu.nova.mechanical_press.press_plates")
                } else {
                    if (this@MechanicalPress.type == PressType.GEAR) NovaMaterialRegistry.GEAR_OFF_BUTTON.createItemBuilder()
                    else NovaMaterialRegistry.GEAR_ON_BUTTON.item.createItemBuilder("menu.nova.mechanical_press.press_gears")
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