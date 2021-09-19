package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.BufferEnergyHolder
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

open class PowerCell(
    creative: Boolean,
    val maxEnergy: Int,
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    final override val energyHolder = BufferEnergyHolder(this, maxEnergy, creative) { createEnergySideConfig(BUFFER) }
    
    override val gui = lazy { PowerCellGUI() }
    
    override fun handleTick() = Unit
    
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
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3, energyHolder)
        
    }
    
}

private val BASIC_CAPACITY = NovaConfig[NovaMaterialRegistry.BASIC_POWER_CELL].getInt("capacity")!!
private val ADVANCED_CAPACITY = NovaConfig[NovaMaterialRegistry.ADVANCED_POWER_CELL].getInt("capacity")!!
private val ELITE_CAPACITY = NovaConfig[NovaMaterialRegistry.ELITE_POWER_CELL].getInt("capacity")!!
private val ULTIMATE_CAPACITY = NovaConfig[NovaMaterialRegistry.ULTIMATE_POWER_CELL].getInt("capacity")!!

class BasicPowerCell(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : PowerCell(
    false,
    BASIC_CAPACITY,
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
    ADVANCED_CAPACITY,
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
    ELITE_CAPACITY,
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
    ULTIMATE_CAPACITY,
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
