package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.ui.EnergyBar
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
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy {
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java))
        { if (it == BlockFace.DOWN) EnergyConnectionType.PROVIDE else EnergyConnectionType.NONE }
    }
    
    private val obstructionTask = runTaskTimer(0, 20 * 5, ::checkSkyObstruction)
    private var obstructed = true
    
    private val gui by lazy { SolarPanelUI() }
    
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
        if (!obstructed) {
            val time = location.world!!.time
            if (time < 13_000) {
                val bestTime = 6_500
                val multiplier = (bestTime - abs(bestTime - time)) / bestTime.toDouble()
                val energyGenerated = (ENERGY_PER_TICK * multiplier).roundToInt()
                energy = min(energy + energyGenerated, MAX_ENERGY)
            }
        }
        if (hasEnergyChanged) gui.energyBar.update()
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    override fun handleDisabled() {
        super.handleDisabled()
        obstructionTask.cancel()
    }
    
    private inner class SolarPanelUI {
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Solar Panel", gui).show()
        }
        
    }
    
}