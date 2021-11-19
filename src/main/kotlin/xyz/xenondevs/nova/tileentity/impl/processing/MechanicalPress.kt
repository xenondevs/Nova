package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.CustomNovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.MECHANICAL_PRESS
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.CONSUME
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.PressProgressItem
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.max

private val MAX_ENERGY = NovaConfig[MECHANICAL_PRESS].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[MECHANICAL_PRESS].getLong("energy_per_tick")!!
private val PRESS_SPEED = NovaConfig[MECHANICAL_PRESS].getInt("speed")!!

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
    
    private val inputInv = getInventory("input", 1, ::handleInputUpdate)
    private val outputInv = getInventory("output", 1, ::handleOutputUpdate)
    
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(CONSUME, FRONT) }
    override val itemHolder = NovaItemHolder(this, inputInv to NetworkConnectionType.BUFFER, outputInv to NetworkConnectionType.EXTRACT)
    
    private var type: PressType = retrieveEnum("pressType") { PressType.PLATE }
    private var timeLeft: Int = retrieveData("pressTime") { 0 }
    private var pressSpeed = 0
    
    private var currentRecipe: CustomNovaRecipe? =
        retrieveOrNull<NamespacedKey>("currentRecipe")?.let {
            when (type) {
                PressType.PLATE -> RecipeManager.platePressRecipes[it]
                PressType.GEAR -> RecipeManager.gearPressRecipes[it]
            }
        }
    
    init {
        handleUpgradeUpdates()
        if (currentRecipe == null) timeLeft = 0
    }
    
    private fun handleUpgradeUpdates() {
        pressSpeed = (PRESS_SPEED * upgradeHolder.getSpeedModifier()).toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (timeLeft == 0) takeItem()
            if (timeLeft != 0) { // is pressing
                timeLeft = max(timeLeft - pressSpeed, 0)
                energyHolder.energy -= energyHolder.energyConsumption
                
                if (timeLeft == 0) {
                    outputInv.putItemStack(null, 0, currentRecipe!!.resultStack)
                    currentRecipe = null
                }
                
                if (gui.isInitialized()) gui.value.updateProgress()
            }
        }
    }
    
    private fun takeItem() {
        val inputItem = inputInv.getItemStack(0)
        if (inputItem != null) {
            val recipe = RecipeManager.getPressRecipeFor(inputItem, type)
            if (recipe != null && outputInv.canHold(recipe.resultStack)) {
                inputInv.addItemAmount(null, 0, -1)
                timeLeft = recipe.time
                currentRecipe = recipe
            }
        }
    }
    
    private fun handleInputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null
            && event.newItemStack != null
            && RecipeManager.getPressRecipeFor(event.newItemStack, type) == null) {
            
            event.isCancelled = true
        }
    }
    
    private fun handleOutputUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != null && event.newItemStack != null) event.isCancelled = true
    }
    
    override fun saveData() {
        super.saveData()
        storeData("pressType", type)
        storeData("pressTime", timeLeft)
        storeData("currentRecipe", currentRecipe?.key)
    }
    
    
    inner class MechanicalPressGUI : TileEntityGUI() {
        
        private val pressProgress = PressProgressItem()
        private val pressTypeItems = ArrayList<PressTypeItem>()
        
        private val sideConfigGUI = SideConfigGUI(
            this@MechanicalPress,
            listOf(NONE, CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inputInv) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInv) to "inventory.nova.output",
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
            val recipeTime = currentRecipe?.time ?: 0
            pressProgress.percentage = if (timeLeft == 0) 0.0 else (recipeTime - timeLeft).toDouble() / recipeTime.toDouble()
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