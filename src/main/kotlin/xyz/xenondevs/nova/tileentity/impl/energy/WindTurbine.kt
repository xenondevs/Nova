package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import net.minecraft.core.Rotations
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.WIND_TURBINE
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.abs

private val MAX_ENERGY = NovaConfig[WIND_TURBINE].getInt("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[WIND_TURBINE].getInt("energy_per_tick")!!

class WindTurbine(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { WindTurbineGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, ::updateEnergyPerTick, UpgradeType.EFFICIENCY, UpgradeType.ENERGY)
    override val energyHolder = ProviderEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, upgradeHolder) {
        createEnergySideConfig(
            EnergyConnectionType.PROVIDE,
            BlockSide.TOP, BlockSide.RIGHT, BlockSide.LEFT, BlockSide.BACK
        )
    }
    
    private val columnModel = createMultiModel()
    private val turbineModel = createMultiModel()
    
    private val altitude = (location.y + abs(world.minHeight)) / (world.maxHeight - 1 + abs(world.minHeight))
    private val rotationPerTick = altitude.toFloat() * 15
    private var energyPerTick = 0
    
    init {
        updateEnergyPerTick()
        spawnModels()
    }
    
    private fun updateEnergyPerTick() {
        energyPerTick = (altitude * energyHolder.energyGeneration).toInt()
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        
        setAdditionalHitboxes(first, getMultiHitboxLocations(location))
    }
    
    private fun spawnModels() {
        val location = armorStand.location.clone()
        location.y += 2
        columnModel.addModels(Model(WIND_TURBINE.block!!.createItemStack(1), location))
        
        location.y += 1.0 / 32.0
        turbineModel.addModels(Model(WIND_TURBINE.block.createItemStack(2), location))
        
        for (blade in 0..2) {
            turbineModel.addModels(Model(
                WIND_TURBINE.block.createItemStack(3),
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
        
        energyHolder.energy += energyPerTick
    }
    
    companion object {
        
        fun canPlace(player: Player, location: Location) =
            getMultiHitboxLocations(location).all { it.block.type.isAir && ProtectionManager.canPlace(player, it) }
        
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
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 4, y = 1, height = 3, energyHolder)
        
    }
    
}