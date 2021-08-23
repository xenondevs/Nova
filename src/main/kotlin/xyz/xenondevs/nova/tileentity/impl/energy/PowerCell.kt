package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

open class PowerCell(
    private val creative: Boolean,
    val maxEnergy: Int,
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(BUFFER) }
    override val requestedEnergy: Int
        get() = if (creative) Int.MAX_VALUE else maxEnergy - energy
    
    override val gui by lazy { PowerCellGUI() }
    
    init {
        if (creative) energy = Int.MAX_VALUE
    }
    
    override fun handleTick() {
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    override fun addEnergy(energy: Int) {
        if (!creative) {
            super.addEnergy(energy)
        }
    }
    
    override fun removeEnergy(energy: Int) {
        if (!creative) {
            super.removeEnergy(energy)
        }
    }
    
    inner class PowerCellGUI : TileEntityGUI("menu.nova.power_cell") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@PowerCell,
            listOf(NONE, PROVIDE, CONSUME, BUFFER),
            null
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, maxEnergy, -1) }
        
    }
    
}

class BasicPowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.basic.capacity")!!,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class AdvancedPowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.advanced.capacity")!!,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class ElitePowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.elite.capacity")!!,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class UltimatePowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.ultimate.capacity")!!,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)

class CreativePowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    true,
    Int.MAX_VALUE,
    uuid,
    data,
    material,
    ownerUUID,
    armorStand,
)
