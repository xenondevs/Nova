package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import java.util.*

open class PowerCell(
    private val creative: Boolean,
    val maxEnergy: Int,
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand,
) : EnergyTileEntity(ownerUUID, material, data, armorStand) {
    
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
    
    inner class PowerCellGUI : TileEntityGUI("Power Cell") {
        
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
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.basic.capacity")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class AdvancedPowerCell(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.advanced.capacity")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class ElitePowerCell(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.elite.capacity")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class UltimatePowerCell(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : PowerCell(
    false,
    NovaConfig.getInt("power_cell.ultimate.capacity")!!,
    ownerUUID,
    material,
    data,
    armorStand
)

class CreativePowerCell(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : PowerCell(
    true,
    Int.MAX_VALUE,
    ownerUUID,
    material,
    data,
    armorStand
)
