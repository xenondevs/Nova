package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SOLAR_PANEL
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.isGlass
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.untilHeightLimit
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig[SOLAR_PANEL].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[SOLAR_PANEL].getLong("energy_per_tick")!!

class SolarPanel(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { SolarPanelGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder) {
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java))
        { if (it == BlockFace.DOWN) EnergyConnectionType.PROVIDE else EnergyConnectionType.NONE }
    }
    
    private val obstructionTask = runTaskTimer(0, 20 * 5, ::checkSkyObstruction)
    private var obstructed = true
    
    private fun checkSkyObstruction() {
        obstructed = false
        location.untilHeightLimit(false) {
            val material = it.block.type
            if (material != Material.AIR && !material.isGlass()) {
                obstructed = true
                return@untilHeightLimit false
            }
            return@untilHeightLimit true
        }
    }
    
    override fun handleTick() {
        energyHolder.energy += calculateCurrentEnergyOutput()
    }
    
    private fun calculateCurrentEnergyOutput(): Int {
        val time = location.world!!.time
        if (!obstructed && time < 13_000) {
            val bestTime = 6_500
            val multiplier = (bestTime - abs(bestTime - time)) / bestTime.toDouble()
            return (energyHolder.energyGeneration * multiplier).roundToInt()
        }
        return 0
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        obstructionTask.cancel()
    }
    
    inner class SolarPanelGUI : TileEntityGUI("menu.nova.solar_panel") {
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| u # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3, energyHolder)
        
    }
    
}