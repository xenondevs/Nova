package xyz.xenondevs.nova.tileentity.impl.energy

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.LAVA_GENERATOR
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ProviderEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.util.*

private val ENERGY_CAPACITY = NovaConfig[LAVA_GENERATOR].getLong("energy_capacity")!!
private val FLUID_CAPACITY = NovaConfig[LAVA_GENERATOR].getLong("fluid_capacity")!!
private val ENERGY_PER_MB = NovaConfig[LAVA_GENERATOR].getDouble("energy_per_mb")!!
private val BURN_RATE = NovaConfig[LAVA_GENERATOR].getDouble("burn_rate")!!

class LavaGenerator(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::LavaGeneratorGUI)
    
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.EFFICIENCY, UpgradeType.SPEED, UpgradeType.ENERGY, UpgradeType.FLUID)
    private val fluidContainer = getFluidContainer("tank", hashSetOf(FluidType.LAVA), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    override val fluidHolder = NovaFluidHolder(this, fluidContainer to NetworkConnectionType.BUFFER, defaultConnectionConfig = { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) })
    override val energyHolder = ProviderEnergyHolder(this, ENERGY_CAPACITY, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.PROVIDE, BlockSide.FRONT) }
    
    private var on = false
    private var burnRate = 0.0
    private var burnProgress = 0.0
    private var energyPerTick = 0L
    
    private val smokeParticleTask = createParticleTask(listOf(
        particle(ParticleEffect.SMOKE_NORMAL) {
            location(armorStand.location.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
            speed(0f)
            amount(1)
        }
    ), 3)
    
    private val lavaParticleTask = createParticleTask(listOf(
        particle(ParticleEffect.LAVA) {
            location(armorStand.location.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
        }
    ), 200)
    
    init {
        handleUpgradeUpdates()
    }
    
    override fun getHeadStack(): ItemStack {
        return material.block!!.createItemStack(on.intValue)
    }
    
    private fun handleUpgradeUpdates() {
        burnRate = BURN_RATE * upgradeHolder.getSpeedModifier() / upgradeHolder.getEfficiencyModifier()
        energyPerTick = (ENERGY_PER_MB * BURN_RATE * upgradeHolder.getSpeedModifier()).toLong()
    }
    
    override fun handleTick() {
        if (energyHolder.energy == energyHolder.maxEnergy || fluidContainer.isEmpty()) {
            if (on) {
                on = false
                updateHeadStack()
                smokeParticleTask.stop()
                lavaParticleTask.stop()
            }
            
            return
        } else if (!on) {
            on = true
            updateHeadStack()
            smokeParticleTask.start()
            lavaParticleTask.start()
        }
        
        val lavaAmount = fluidContainer.amount
        if (lavaAmount >= burnRate) {
            energyHolder.energy += energyPerTick
            
            burnProgress += burnRate
            if (burnProgress > 1) {
                val burnt = burnProgress.toLong()
                
                burnProgress -= burnt
                fluidContainer.takeFluid(burnt)
            }
        } else {
            energyHolder.energy += (lavaAmount * ENERGY_PER_MB).toLong()
            fluidContainer.clear()
        }
    }
    
    inner class LavaGeneratorGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@LavaGenerator,
            fluidContainers = listOf(fluidContainer to "container.nova.lava_tank"),
            openPrevious = ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # f e |" +
                "| u # # # # f e |" +
                "| # # # # # f e |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        init {
            FluidBar(gui, x = 6, y = 1, height = 3, fluidContainer)
            EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        }
        
    }
    
}