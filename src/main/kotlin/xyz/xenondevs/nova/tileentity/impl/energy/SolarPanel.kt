package xyz.xenondevs.nova.tileentity.impl.energy

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.isGlass
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.untilHeightLimit
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig.getInt("solar_panel.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("solar_panel.energy_per_tick")!!

class SolarPanel(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy {
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java))
        { if (it == BlockFace.DOWN) EnergyConnectionType.PROVIDE else EnergyConnectionType.NONE }
    }
    
    private val obstructionTask = runTaskTimer(0, 20 * 5, ::checkSkyObstruction)
    private var obstructed = true
    
    override val gui by lazy { SolarPanelGUI() }
    
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
        energy = min(energy + calculateCurrentEnergyOutput(), MAX_ENERGY)
        if (hasEnergyChanged) gui.energyBar.update()
    }
    
    private fun calculateCurrentEnergyOutput(): Int {
        val time = location.world!!.time
        if (!obstructed && time < 13_000) {
            val bestTime = 6_500
            val multiplier = (bestTime - abs(bestTime - time)) / bestTime.toDouble()
            return (ENERGY_PER_TICK * multiplier).roundToInt()
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
            .addIngredient('u', UpgradesTeaserItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, MAX_ENERGY, calculateCurrentEnergyOutput()) }
        
    }
    
}