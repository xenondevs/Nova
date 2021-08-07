package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import net.minecraft.core.Rotations
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*
import kotlin.math.abs
import kotlin.math.min

private val MAX_ENERGY = NovaConfig.getInt("wind_turbine.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("wind_turbine.energy_per_tick")!!

class WindTurbine(
    uuid: UUID,
    data: CompoundElement,
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
    
    private val columnModel = createMultiModel()
    private val turbineModel = createMultiModel()
    
    private val altitude = (location.y + abs(world.minHeight)) / (world.maxHeight - 1 + abs(world.minHeight))
    private val energyPerTick = (altitude * ENERGY_PER_TICK).toInt()
    private val rotationPerTick = altitude.toFloat() * 5
    
    init {
        spawnModels()
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        
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
                Rotations(0f, 0f, blade * 120f)
            ))
        }
        
        turbineModel.useArmorStands {
            it.setHeadPose(it.headPose.copy(x = 90f))
            it.updateEntityData()
        }
    }
    
    override fun handleTick() {
        runAsyncTask {
            turbineModel.useArmorStands {
                it.setHeadPose(it.headPose.add(0f, 0f, rotationPerTick))
                it.updateEntityData()
            }
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