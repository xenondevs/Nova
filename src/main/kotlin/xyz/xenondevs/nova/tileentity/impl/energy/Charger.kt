package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.item.impl.ChargeableItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.CHARGER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY = NovaConfig[CHARGER].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[CHARGER].getLong("charge_speed")!!

class Charger(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui = lazy { ChargerGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, UpgradeType.ENERGY, UpgradeType.SPEED)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.BUFFER)
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd && event.newItemStack.novaMaterial?.novaItem !is ChargeableItem) event.isCancelled = true
    }
    
    override fun handleTick() {
        val currentItem = inventory.getUnsafeItemStack(0)
        val novaItem = currentItem?.novaMaterial?.novaItem
        if (novaItem is ChargeableItem) {
            val itemCharge = novaItem.getEnergy(currentItem)
            if (itemCharge < novaItem.maxEnergy) {
                val chargeEnergy = minOf(energyHolder.energyConsumption, energyHolder.energy, novaItem.maxEnergy - itemCharge)
                novaItem.addEnergy(currentItem, chargeEnergy)
                energyHolder.energy -= chargeEnergy
                
                inventory.notifyWindows()
            }
        }
    }
    
    inner class ChargerGUI : TileEntityGUI("menu.nova.charger") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Charger,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default")
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
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        
    }
    
}