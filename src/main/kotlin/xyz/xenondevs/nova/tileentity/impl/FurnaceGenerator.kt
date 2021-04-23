package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.PROVIDE
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.ItemStorage
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.network.item.inventory.NetworkedVirtualInventory
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenSideConfigItem
import xyz.xenondevs.nova.ui.SideConfigGUI
import xyz.xenondevs.nova.ui.item.EnergyProgressItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.BlockSide.FRONT
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_ENERGY = 10_000
private const val ENERGY_PER_TICK = 20
private const val BURN_TIME_MULTIPLIER = 0.1

class FurnaceGenerator(
    material: NovaMaterial,
    armorStand: ArmorStand
) : TileEntity(material, armorStand), EnergyStorage, ItemStorage {
    
    private var energy: Int = retrieveData(0, "energy")
    private var burnTime: Int = retrieveData(0, "burnTime")
    private var totalBurnTime: Int = retrieveData(0, "totalBurnTime")
    
    private val inventory = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("inventory"), 1)
        .also { it.setItemUpdateHandler(this::handleInventoryUpdate) }
    private val gui by lazy { CoalGeneratorGUI() }
    private var updateEnergyBar = true
    
    override val networks = EnumMap<NetworkType, MutableMap<BlockFace, Network>>(NetworkType::class.java)
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> =
        retrieveData(createEnergySideConfig(PROVIDE, FRONT), "energyConfig")
    override val requestedEnergy = 0
    override val providedEnergy: Int
        get() = energy
    
    override val inventories: MutableMap<BlockFace, NetworkedInventory>
    override val itemConfig: MutableMap<BlockFace, ItemConnectionType> =
        retrieveData(createItemSideConfig(ItemConnectionType.INSERT, FRONT), "itemConfig")
    
    init {
        val networkedInventory = NetworkedVirtualInventory(inventory)
        inventories = CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java)) { networkedInventory }
    }
    
    override fun addEnergy(energy: Int) {
        throw UnsupportedOperationException()
    }
    
    override fun removeEnergy(energy: Int) {
        this.energy -= energy
        updateEnergyBar = true
    }
    
    override fun handleTick() {
        if (burnTime == 0) burnItem()
        
        if (burnTime != 0) {
            burnTime--
            energy = min(MAX_ENERGY, energy + ENERGY_PER_TICK)
            
            gui.progressItem.percentage = burnTime.toDouble() / totalBurnTime.toDouble()
            updateEnergyBar = true
        }
        
        if (updateEnergyBar) {
            gui.energyBar.update()
            updateEnergyBar = false
        }
    }
    
    override fun handleInitialized() {
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        NetworkManager.handleEndPointRemove(this, unload)
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
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val drops = super.destroy(dropItems)
        
        // add items from inventory if there are any
        if (inventory.hasItemStack(0)) {
            drops += inventory.getItemStack(0)
        }
        
        // delete the inventory since the items will be dropped
        VirtualInventoryManager.getInstance().remove(inventory)
        
        return drops
    }
    
    override fun saveData() {
        storeData("energy", energy, true)
        storeData("burnTime", burnTime)
        storeData("totalBurnTime", totalBurnTime)
        storeData("energyConfig", energyConfig)
        storeData("itemConfig", itemConfig)
    }
    
    private fun getEnergyValues() = energy to MAX_ENERGY
    
    inner class CoalGeneratorGUI {
        
        val progressItem = EnergyProgressItem()
        
        private val sideConfigGUI = SideConfigGUI(this@FurnaceGenerator, NONE, PROVIDE) { openWindow(it) }
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
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4, ::getEnergyValues)
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Furnace Generator", gui).show()
        }
    }
    
    companion object {
        
        fun createItemBuilder(material: NovaMaterial, tileEntity: TileEntity?): ItemBuilder {
            val builder = material.createBasicItemBuilder()
            val energy = tileEntity?.let { (tileEntity as FurnaceGenerator).energy } ?: 0
            builder.addLoreLines(EnergyUtils.getEnergyString(energy, MAX_ENERGY))
            return builder
        }
        
    }
    
}
