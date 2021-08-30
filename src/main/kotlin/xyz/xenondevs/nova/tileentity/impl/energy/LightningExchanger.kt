package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.weather.LightningStrikeEvent
import org.bukkit.event.weather.LightningStrikeEvent.Cause
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType.PROVIDE
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.min
import kotlin.random.Random

private val MAX_ENERGY = NovaConfig.getInt("lightning_exchanger.capacity")!!
private val CONVERSION_RATE = NovaConfig.getInt("lightning_exchanger.conversion_rate")!!
private val MIN_BURST = NovaConfig.getInt("lightning_exchanger.burst.min")!!
private val MAX_BURST = NovaConfig.getInt("lightning_exchanger.burst.max")!!

class LightningExchanger(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { LightningExchangerGUI() }
    override val upgradeHolder = UpgradeHolder(data, gui, ::handleUpgradeUpdates, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, 0, upgradeHolder) {
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java))
        { if (it == BlockFace.DOWN) PROVIDE else NONE }
    }
    
    private var minBurst = 0
    private var maxBurst = 0
    private var toCharge = 0
    
    init {
        handleUpgradeUpdates()
    }
    
    private fun handleUpgradeUpdates() {
        minBurst = (MIN_BURST * upgradeHolder.getEfficiencyModifier()).toInt()
        maxBurst = (MAX_BURST * upgradeHolder.getEfficiencyModifier()).toInt()
    }
    
    override fun handleTick() {
        val charge = min(CONVERSION_RATE, toCharge)
        energyHolder.energy += charge
        toCharge -= charge
    }
    
    fun addEnergyBurst() {
        val leeway = energyHolder.maxEnergy - energyHolder.energy - toCharge
        toCharge += (if (leeway <= maxBurst) leeway else Random.nextInt(minBurst, maxBurst))
    }
    
    inner class LightningExchangerGUI : TileEntityGUI("menu.nova.lightning_exchanger") {
        
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
    
    private companion object LightningHandler : Listener {
        
        init {
            Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        }
        
        @EventHandler
        fun handleLightning(event: LightningStrikeEvent) {
            val struckBlock = event.lightning.location.advance(BlockFace.DOWN).block
            if (event.cause != Cause.WEATHER || struckBlock.type != Material.LIGHTNING_ROD)
                return
            val tile = TileEntityManager.getTileEntityAt(struckBlock.location.advance(BlockFace.DOWN), false)
            if (tile !is LightningExchanger)
                return
            tile.addEnergyBurst()
            
        }
    }
}
