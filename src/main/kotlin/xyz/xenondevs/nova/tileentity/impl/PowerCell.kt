package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.energy.EnergyNetwork
import xyz.xenondevs.nova.energy.EnergyNetworkManager
import xyz.xenondevs.nova.energy.EnergyStorage
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenSideConfigItem
import xyz.xenondevs.nova.ui.SideConfigGUI
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.color.RegularColor
import java.awt.Color
import java.util.*

private const val MAX_ENERGY = 100_000

class PowerCell(
    material: NovaMaterial,
    armorStand: ArmorStand,
) : TileEntity(material, armorStand, true), EnergyStorage {
    
    private var storedEnergy = retrieveData(0, "storedEnergy")
    private val gui = PowerCellUI()
    private var updateEnergyBar = true
    
    override val networks = EnumMap<BlockFace, EnergyNetwork>(BlockFace::class.java)
    override val configuration = retrieveData(createSideConfig(BUFFER), "sideConfig")
    override val providedEnergy: Int
        get() = storedEnergy
    override val requestedEnergy
        get() = MAX_ENERGY - storedEnergy
    
    override fun addEnergy(energy: Int) {
        storedEnergy += energy
        updateEnergyBar = true
    }
    
    override fun removeEnergy(energy: Int) {
        storedEnergy -= energy
        updateEnergyBar = true
    }
    
    override fun handleInitialized() {
        EnergyNetworkManager.handleStorageAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        EnergyNetworkManager.handleStorageRemove(this, unload)
    }
    
    override fun saveData() {
        storeData("storedEnergy", storedEnergy)
        storeData("sideConfig", configuration) // TODO: don't save sideConfig in item
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    private fun getEnergyValues() = storedEnergy to MAX_ENERGY
    
    override fun handleTick() {
        if (updateEnergyBar) {
            gui.energyBar.update()
            updateEnergyBar = false
        }
        
        configuration.forEach { (face, _) ->
            val network = networks[face]
            ParticleBuilder(ParticleEffect.REDSTONE, armorStand.location.clone().add(0.0, 0.5, 0.0).advance(face, 0.5))
                .setParticleData(network?.color ?: RegularColor(Color(Color.HSBtoRGB(0f, 0f, 0f))))
                .display()
        }
    }
    
    private inner class PowerCellUI {
        
        private val sideConfigGUI = SideConfigGUI(this@PowerCell, NONE, CONSUME, PROVIDE, BUFFER) { openWindow(it) }
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3, ::getEnergyValues)
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Power Cell", gui).show()
        }
        
    }
    
}