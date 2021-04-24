package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.CONSUME
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.recipe.PressRecipe
import xyz.xenondevs.nova.recipe.PressType
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.PressProgressItem
import xyz.xenondevs.nova.util.BlockSide.FRONT
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.seed
import java.util.*

private const val MAX_ENERGY = 5_000
private const val ENERGY_PER_TICK = 100
private const val PRESS_TIME = 200

class MechanicalPress(
    material: NovaMaterial,
    armorStand: ArmorStand
) : TileEntity(material, armorStand), EnergyStorage, ItemStorage {
    
    // --- Mechanical Press ---
    private var energy = retrieveData(0, "energy")
    private var type: PressType = retrieveData(PressType.PLATE, "pressType")
    private var pressTime: Int = 0
    private var currentItem: ItemStack? = null
    
    private val inputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("input"), 1).apply { setItemUpdateHandler(::handleInputUpdate) }
    private val outputInv = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("output"), 1).apply { setItemUpdateHandler(::handleOutputUpdate) }
    
    // --- Network ---
    override val networks = EnumMap<NetworkType, MutableMap<BlockFace, Network>>(NetworkType::class.java)
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> = retrieveData(createEnergySideConfig(CONSUME, FRONT), "energyConfig")
    override val providedEnergy = 0
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val networkedInputInv = NetworkedVirtualInventory(inputInv)
    private val networkedOutputInv = NetworkedVirtualInventory(outputInv)
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory> =
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { networkedInputInv }
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData(createItemSideConfig(ItemConnectionType.INSERT, FRONT), "itemConfig")
    
    // -- GUI ---
    private val gui by lazy { MechanicalPressUI() }
    private var updateEnergyBar = true
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) { // has energy to do anything
            if (pressTime == 0) takeItem()
            if (pressTime != 0) { // is pressing
                pressTime--
                energy -= ENERGY_PER_TICK
                
                if (pressTime == 0) {
                    outputInv.placeOne(null, 0, currentItem)
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
            val recipeOutput = PressRecipe.getOutputFor(inputItem.type, type)
            val outputStack = recipeOutput.createItemStack()
            if (outputInv.simulateAdd(outputStack) == 0) {
                inputInv.removeOne(null, 0)
                currentItem = outputStack
                pressTime = PRESS_TIME
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
            if (!PressRecipe.isPressable(material, type)) {
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
        storeData("pressType", pressTime)
        storeData("energyConfig", energyConfig)
        storeData("itemConfig", itemConfig)
    }
    
    override fun handleInitialized() {
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
    private fun getEnergyValues() = energy to MAX_ENERGY
    
    inner class MechanicalPressUI {
        
        private val pressProgress = PressProgressItem()
        private val pressTypeItems = ArrayList<PressTypeItem>()
        
        private val sideConfigGUI = SideConfigGUI(
            this@MechanicalPress,
            listOf(NONE, EnergyConnectionType.CONSUME),
            listOf(networkedInputInv to "Input Inventory", networkedOutputInv to "Output Inventory")
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
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
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, ::getEnergyValues)
        
        fun updateProgress() {
            pressProgress.percentage = if (pressTime == 0) 0.0 else (PRESS_TIME - pressTime).toDouble() / PRESS_TIME.toDouble()
        }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Mechanical Press", gui).show()
        }
        
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