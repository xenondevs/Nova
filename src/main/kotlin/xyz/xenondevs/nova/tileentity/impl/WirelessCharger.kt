package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.impl.ChargeableItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.region.Region
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.novaMaterial
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("wireless_charger.capacity")!!
private val CHARGE_SPEED = NovaConfig.getInt("wireless_charger.charge_speed")!!
private val RANGE = NovaConfig.getDouble("wireless_charger.range")!!

class WirelessCharger(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val gui by lazy(::WirelessChargerGUI)
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val region = Region(
        location.clone().center().subtract(RANGE, RANGE, RANGE),
        location.clone().center().add(RANGE, RANGE, RANGE)
    )
    
    override fun handleTick() {
        var energyTransferred = 0
        
        if (energy != 0) {
            chargeLoop@ for (entity in world.entities) {
                if (entity.location in region) {
                    if (entity is Player) {
                        for (itemStack in entity.inventory) {
                            energyTransferred += chargeItemStack(energyTransferred, itemStack)
                            if (energyTransferred == CHARGE_SPEED || energy == 0) break@chargeLoop
                        }
                    } else if (entity is Item) {
                        energyTransferred += chargeItemStack(energyTransferred, entity.itemStack)
                        if (energyTransferred == CHARGE_SPEED || energy == 0) break@chargeLoop
                    }
                }
            }
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    private fun chargeItemStack(alreadyTransferred: Int, itemStack: ItemStack?): Int {
        val novaItem = itemStack?.novaMaterial?.novaItem
        
        if (novaItem is ChargeableItem) {
            val maxEnergy = novaItem.maxEnergy
            val currentEnergy = novaItem.getEnergy(itemStack)
            
            val energyToTransfer = minOf(CHARGE_SPEED - alreadyTransferred, maxEnergy - currentEnergy, energy)
            energy -= energyToTransfer
            novaItem.addEnergy(itemStack, energyToTransfer)
            
            return energyToTransfer
        }
        
        return 0
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class WirelessChargerGUI : TileEntityGUI("menu.nova.wireless_charger") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@WirelessCharger,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.NONE),
            null
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s v # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('v', VisualizeRegionItem(uuid, region))
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
    }
    
}