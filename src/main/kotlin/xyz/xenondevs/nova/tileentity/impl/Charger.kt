package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.impl.ChargeableItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.novaMaterial
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("charger.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("charger.charge_speed")!!

class Charger(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 1, true, ::handleInventoryUpdate)
    
    private val gui by lazy { ChargerGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd && event.newItemStack.novaMaterial?.novaItem !is ChargeableItem) event.isCancelled = true
    }
    
    override fun handleTick() {
        val currentItem = inventory.getItemStack(0)
        val novaItem = currentItem?.novaMaterial?.novaItem
        if (novaItem is ChargeableItem) {
            val itemCharge = novaItem.getEnergy(currentItem)
            if (itemCharge < novaItem.maxEnergy) {
                val chargeEnergy = minOf(ENERGY_PER_TICK, energy, novaItem.maxEnergy - itemCharge)
                novaItem.addEnergy(currentItem, chargeEnergy)
                energy -= chargeEnergy
                
                inventory.notifyWindows()
            }
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    private inner class ChargerGUI {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Charger,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "Charger Inventory", ItemConnectionType.ALL_TYPES))
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # . |" +
                "| # # # i # # . |" +
                "| # # # # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('i', VISlotElement(inventory, 0))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Charger", gui).show()
        }
        
    }
    
}