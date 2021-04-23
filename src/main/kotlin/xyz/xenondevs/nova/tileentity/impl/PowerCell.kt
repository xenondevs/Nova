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
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.*
import xyz.xenondevs.nova.network.energy.EnergyStorage
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenSideConfigItem
import xyz.xenondevs.nova.ui.SideConfigGUI
import xyz.xenondevs.nova.util.EnergyUtils
import java.util.*

open class PowerCell(
    val maxEnergy: Int,
    material: NovaMaterial,
    armorStand: ArmorStand,
) : TileEntity(material, armorStand), EnergyStorage {
    
    private var storedEnergy = retrieveData(0, "storedEnergy")
    private val gui = PowerCellUI()
    private var updateEnergyBar = true
    
    override val networks = EnumMap<NetworkType, MutableMap<BlockFace, Network>>(NetworkType::class.java)
    override val energyConfig: MutableMap<BlockFace, EnergyConnectionType> = retrieveData(createEnergySideConfig(BUFFER), "sideConfig")
    override val allowedFaces: Map<NetworkType, List<BlockFace>>
        get() = mapOf(NetworkType.ENERGY to energyConfig.filterNot { it.value == NONE }.map { it.key })
    override val providedEnergy: Int
        get() = storedEnergy
    override val requestedEnergy
        get() = maxEnergy - storedEnergy
    
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
    
    private fun getEnergyValues() = storedEnergy to maxEnergy
    
    override fun handleTick() {
        if (updateEnergyBar) {
            gui.energyBar.update()
            updateEnergyBar = false
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
            val maxEnergy = tileEntity?.let { (tileEntity as PowerCell).maxEnergy } ?: 0
            builder.addLoreLines(EnergyUtils.getEnergyString(energy, maxEnergy))
            return builder
        }
        
    }
    
}

class BasicPowerCell(material: NovaMaterial, armorStand: ArmorStand) : PowerCell(100_000, material, armorStand)

class AdvancedPowerCell(material: NovaMaterial, armorStand: ArmorStand) : PowerCell(1_000_000, material, armorStand)

class ElitePowerCell(material: NovaMaterial, armorStand: ArmorStand) : PowerCell(5_000_000, material, armorStand)

class UltimatePowerCell(material: NovaMaterial, armorStand: ArmorStand) : PowerCell(20_000_000, material, armorStand)

class CreativePowerCell(material: NovaMaterial, armorStand: ArmorStand) : PowerCell(Int.MAX_VALUE, material, armorStand)
