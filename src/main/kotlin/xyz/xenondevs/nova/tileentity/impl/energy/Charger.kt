package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.item.impl.ChargeableItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("charger.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("charger.charge_speed")!!

class Charger(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 1, true, ::handleInventoryUpdate)
    
    override val gui by lazy { ChargerGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd && event.newItemStack.novaMaterial?.novaItem !is ChargeableItem) event.isCancelled = true
    }
    
    override fun handleTick() {
        val currentItem = inventory.getUnsafeItemStack(0)
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
    
    inner class ChargerGUI : TileEntityGUI("menu.nova.charger") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Charger,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # . |" +
                "| u # # i # # . |" +
                "| # # # # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('i', VISlotElement(inventory, 0))
            .addIngredient('u', UpgradesTeaserItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
    }
    
}