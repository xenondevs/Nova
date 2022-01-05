package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Farmland
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPRINKLER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import xyz.xenondevs.particle.ParticleEffect
import java.util.*
import kotlin.math.min
import kotlin.math.roundToLong

private val WATER_CAPACITY = NovaConfig[SPRINKLER].getLong("water_capacity")!!
private val WATER_PER_MOISTURE_LEVEL = NovaConfig[SPRINKLER].getLong("water_per_moisture_level")!!
private val MIN_RANGE = NovaConfig[SPRINKLER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[SPRINKLER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[SPRINKLER].getInt("range.default")!!

class Sprinkler(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::SprinklerGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.EFFICIENCY, UpgradeType.FLUID, UpgradeType.RANGE)
    private val tank = getFluidContainer("tank", hashSetOf(FluidType.WATER), WATER_CAPACITY, upgradeHolder = upgradeHolder)
    override val fluidHolder = NovaFluidHolder(this, tank to NetworkConnectionType.BUFFER, defaultConnectionConfig = { createExclusiveSideConfig(NetworkConnectionType.INSERT, BlockSide.BOTTOM) })
    
    private var maxRange = 0
    private var waterPerMoistureLevel = 0L
    
    lateinit var region: Region
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    init {
        handleUpgradeUpdates()
        updateRegion()
        
        sprinklers += this
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        sprinklers -= this
    }
    
    private fun updateRegion() {
        val d = range + 0.5
        region = Region(
            location.clone().center().add(-d, -1.0, -d),
            location.clone().center().add(d, 0.0, d)
        )
        VisualRegion.updateRegion(uuid, region)
    }
    
    private fun handleUpgradeUpdates() {
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        waterPerMoistureLevel = (WATER_PER_MOISTURE_LEVEL / upgradeHolder.getEfficiencyModifier()).roundToLong()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    inner class SprinklerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Sprinkler,
            null, null,
            listOf(tank to "container.nova.fluid_tank"),
            ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # f # # p |" +
                "| u # # f # # d |" +
                "| v # # f # # m |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('v', VisualizeRegionItem(uuid) { region })
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('d', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('f', FluidBar(3, fluidHolder, tank))
            .build()
        
        fun updateRangeItems() {
            rangeItems.forEach(Item::notifyWindows)
        }
        
    }
    
    companion object : Listener {
        
        private val sprinklers = ArrayList<Sprinkler>()
        
        init {
            Bukkit.getPluginManager().registerEvents(this, NOVA)
        }
        
        @EventHandler
        fun handleBlockPhysics(event: BlockPhysicsEvent) {
            if (event.changedType == Material.FARMLAND && event.block == event.sourceBlock) {
                val block = event.block
                val location = block.location.add(0.5, 0.5, 0.5)
                val farmland = block.blockData as Farmland
                if (farmland.moisture >= farmland.maximumMoisture) return
                
                val requiredMoisture = farmland.maximumMoisture - farmland.moisture
                var addedMoisture = 0
                for (sprinkler in sprinklers) {
                    if (location !in sprinkler.region) continue
                    val moistureFromSprinkler = min(requiredMoisture - addedMoisture, (sprinkler.tank.amount / sprinkler.waterPerMoistureLevel).toInt())
                    sprinkler.tank.takeFluid(moistureFromSprinkler * sprinkler.waterPerMoistureLevel)
                    addedMoisture += moistureFromSprinkler
                    if (addedMoisture == requiredMoisture) break
                }
                
                if (addedMoisture > 0) {
                    farmland.moisture += addedMoisture
                    block.setBlockData(farmland, false)
                    
                    showWaterParticles(block, sprinklers[0].getViewers())
                }
            }
        }
        
        private fun showWaterParticles(block: Block, players: List<Player>) {
            particleBuilder(ParticleEffect.WATER_SPLASH, block.location.apply { add(0.5, 1.0, 0.5) }) {
                offset(0.2, 0.1, 0.2)
                speed(1f)
                amount(20)
            }.display(players)
        }
        
    }
    
}