package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.energy.EnergyConnectionType
import xyz.xenondevs.nova.energy.EnergyNetwork
import xyz.xenondevs.nova.energy.EnergyNetworkManager
import xyz.xenondevs.nova.energy.EnergyStorage
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.color.RegularColor
import java.awt.Color
import java.util.*

private const val MAX_ENERGY = 100_000

// TODO: Give PowerCells the ability to provide energy
class PowerCell(
    material: NovaMaterial,
    armorStand: ArmorStand,
) : TileEntity(material, armorStand, true), EnergyStorage {
    
    private var storedEnergy = retrieveData(0, "storedEnergy")
    
    private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
        .setStructure("" +
            "1 - - - - - - - 2" +
            "| # # # . # # # |" +
            "| # # # . # # # |" +
            "| # # # . # # # |" +
            "3 - - - - - - - 4")
        .build()
    
    private val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3, ::getEnergyValues)
    
    override val networks = EnumMap<BlockFace, EnergyNetwork>(BlockFace::class.java)
    override val configuration = CUBE_FACES.map { it to EnergyConnectionType.CONSUME }.toMap(EnumMap(BlockFace::class.java))
    override val providedEnergy = 0
    override val requestedEnergy = MAX_ENERGY - storedEnergy
    
    override fun addEnergy(energy: Int) {
        storedEnergy += energy
        energyBar.percentage = storedEnergy.toDouble() / MAX_ENERGY.toDouble()
    }
    
    override fun removeEnergy(energy: Int) {
        throw UnsupportedOperationException()
    }
    
    override fun handleInitialized() {
        EnergyNetworkManager.handleStorageAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        EnergyNetworkManager.handleStorageRemove(this, unload)
    }
    
    override fun saveData() {
        storeData("storedEnergy", storedEnergy)
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        SimpleWindow(event.player, "Power Cell", gui).show()
    }
    
    private fun getEnergyValues() = storedEnergy to MAX_ENERGY
    override fun handleTick() {
        configuration.forEach { (face, _) ->
            val network = networks[face]
            ParticleBuilder(ParticleEffect.REDSTONE, armorStand.location.clone().add(0.0, 0.5, 0.0).advance(face, 0.5))
                .setParticleData(network?.color ?: RegularColor(Color(Color.HSBtoRGB(0f, 0f, 0f))))
                .display()
        }
    }
    
}