package xyz.xenondevs.nova.tileentity.impl.energy

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.EulerAngle
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.getStraightLine
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = NovaConfig.getInt("wind_turbine.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("wind_turbine.energy_per_tick")!!

class WindTurbine(
    uuid: UUID,
    data: JsonObject,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy {
        createEnergySideConfig(
            EnergyConnectionType.PROVIDE,
            BlockSide.TOP, BlockSide.RIGHT, BlockSide.LEFT, BlockSide.BACK
        )
    }
    override val gui by lazy { WindTurbineGUI() }
    
    private val columnModel = getMultiModel("column")
    private val turbineModel = getMultiModel("turbine")
    
    private val altitude = location.y / (world.maxHeight - 1)
    private val energyPerTick = (altitude * ENERGY_PER_TICK).toInt()
    private val rotationPerTick = altitude * 0.2
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        if (first) spawnModels()
        
        setAdditionalHitboxes(first, getMultiHitboxLocations(location))
    }
    
    private fun spawnModels() {
        val location = armorStand.location.clone()
        location.y += 2
        columnModel.addModels(Model(NovaMaterial.WIND_TURBINE.block!!.getItem(1), location))
        
        location.y += 1.0 / 32.0
        turbineModel.addModels(Model(NovaMaterial.WIND_TURBINE.block.getItem(2), location))
        
        for (blade in 0..2) {
            turbineModel.addModels(Model(
                NovaMaterial.WIND_TURBINE.block.getItem(3),
                location,
                EulerAngle(0.0, 0.0, Math.toRadians(blade * 120.0))
            ))
        }
        
        turbineModel.useArmorStands {
            it.headPose = it.headPose.setX(Math.toRadians(90.0))
        }
    }
    
    override fun handleTick() {
        turbineModel.useArmorStands {
            it.headPose = it.headPose.add(0.0, 0.0, rotationPerTick)
        }
        
        energy = min(MAX_ENERGY, energy + energyPerTick)
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    companion object {
        
        fun canPlace(player: Player, location: Location) =
            getMultiHitboxLocations(location).all { it.block.type.isAir && ProtectionUtils.canPlace(player, it) }
        
        fun getMultiHitboxLocations(location: Location) =
            location.clone().add(0.0, 1.0, 0.0).getStraightLine(Axis.Y, location.blockY + 3)
        
    }
    
    inner class WindTurbineGUI : TileEntityGUI("menu.nova.wind_turbine") {
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| u # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('u', UpgradesTeaserItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3) { Triple(energy, MAX_ENERGY, energyPerTick) }
        
    }
    
}