package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.*
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.network.energy.EnergyNetwork
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenSideConfigItem
import xyz.xenondevs.nova.ui.SideConfigGUI
import xyz.xenondevs.nova.util.EnergyUtils
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
) : TileEntity(material, armorStand), EnergyStorage {
    
    private var storedEnergy = retrieveData(0, "storedEnergy")
    private val gui = PowerCellUI()
    private var updateEnergyBar = true
    
    override val networks = EnumMap<NetworkType, MutableMap<BlockFace, Network>>(NetworkType::class.java)
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> = retrieveData(createSideConfig(BUFFER), "sideConfig")
    override val allowedFaces: Map<NetworkType, List<BlockFace>>
        get() = mapOf(NetworkType.ENERGY to energyConfig.filterNot { it.value == NONE }.map { it.key })
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
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
    override fun saveData() {
        storeData("storedEnergy", storedEnergy, true)
        storeData("sideConfig", energyConfig)
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
        
        energyConfig.forEach { (face, _) ->
            val color = networks[NetworkType.ENERGY]?.get(face)
                .let { if (it == null) RegularColor(Color(0, 0, 0)) else (it as EnergyNetwork).color }
            
            ParticleBuilder(ParticleEffect.REDSTONE, armorStand.location.clone().add(0.0, 0.5, 0.0).advance(face, 0.5))
                .setParticleData(color)
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
    
    companion object {
    
        fun createItemBuilder(material: NovaMaterial, tileEntity: TileEntity?): ItemBuilder {
            val builder = material.createBasicItemBuilder()
            val energy = tileEntity?.let { (tileEntity as PowerCell).storedEnergy } ?: 0
            builder.addLoreLines(EnergyUtils.getEnergyString(energy, MAX_ENERGY))
            return builder
        }
        
    }
    
}