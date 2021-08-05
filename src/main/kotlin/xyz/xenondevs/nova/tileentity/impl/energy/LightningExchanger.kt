package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.weather.LightningStrikeEvent
import org.bukkit.event.weather.LightningStrikeEvent.Cause
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.NONE
import xyz.xenondevs.nova.network.energy.EnergyConnectionType.PROVIDE
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.advance
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
) : EnergyTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy {
        CUBE_FACES.associateWithTo(EnumMap(BlockFace::class.java))
        { if (it == BlockFace.DOWN) PROVIDE else NONE }
    }
    override val gui by lazy { LightningExchangerGUI() }
    
    private var toCharge = 0
    
    override fun handleTick() {
        val charge = min(CONVERSION_RATE, toCharge)
        energy += charge
        toCharge -= charge
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    fun addEnergyBurst() {
        val leeway = MAX_ENERGY - energy - toCharge
        toCharge += (if (leeway <= MAX_BURST) leeway else Random.nextInt(MIN_BURST, MAX_BURST))
    }
    
    inner class LightningExchangerGUI : TileEntityGUI("menu.nova.lightning_exchanger") {
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| u # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('u', UpgradesTeaserItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, MAX_ENERGY, min(5000, toCharge)) }
        
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
            val tile = TileEntityManager.getTileEntityAt(struckBlock.location.advance(BlockFace.DOWN))
            if (tile !is LightningExchanger)
                return
            tile.addEnergyBurst()
            
        }
    }
}
