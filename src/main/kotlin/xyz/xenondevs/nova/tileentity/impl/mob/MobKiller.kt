package xyz.xenondevs.nova.tileentity.impl.mob

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Mob
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.MOB_KILLER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = NovaConfig[MOB_KILLER].getInt("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[MOB_KILLER].getInt("energy_per_tick")!!
private val ENERGY_PER_DAMAGE = NovaConfig[MOB_KILLER].getInt("energy_per_damage")!!
private val IDLE_TIME = NovaConfig[MOB_KILLER].getInt("idle_time")!!
private val KILL_LIMIT = NovaConfig[MOB_KILLER].getInt("kill_limit")!!
private val DAMAGE = NovaConfig[MOB_KILLER].getDouble("damage")!!
private val MIN_RANGE = NovaConfig[MOB_KILLER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[MOB_KILLER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[MOB_KILLER].getInt("range.default")!!

class MobKiller(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { MobCrusherGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_DAMAGE, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    private val fakePlayer = EntityUtils.createFakePlayer(location, ownerUUID, "Mob Killer").bukkitEntity
    
    private var timePassed = 0
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var region: Region
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        if (range > maxRange) range = maxRange
    }
    
    private fun updateRegion() {
        region = getBlockFrontRegion(range, range, 4, -1)
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (timePassed++ >= maxIdleTime) {
                timePassed = 0
                
                val killLimit = min(energyHolder.energy / energyHolder.specialEnergyConsumption, KILL_LIMIT)
                
                location
                    .chunk
                    .getSurroundingChunks(1, includeCurrent = true, ignoreUnloaded = true)
                    .flatMap { it.entities.asList() }
                    .filterIsInstance<Mob>()
                    .filter { it.location in region }
                    .take(killLimit)
                    .forEach { entity ->
                        energyHolder.energy -= energyHolder.specialEnergyConsumption
                        entity.damage(DAMAGE, fakePlayer)
                    }
            }
        }
        
        if (gui.isInitialized())
            gui.value.idleBar.percentage = timePassed / maxIdleTime.toDouble()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGUI : TileEntityGUI("menu.nova.mob_killer") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MobKiller,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            null
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . # . # p |" +
                "| r # . # . # n |" +
                "| u # . # . # m |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid) { region })
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .build()
        
        val energyBar = EnergyBar(gui, x = 3, y = 1, height = 3, energyHolder)
        
        val idleBar = object : VerticalBar(gui, x = 5, y = 1, height = 3, NovaMaterialRegistry.GREEN_BAR) {
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.mob_killer.idle", maxIdleTime - timePassed))
        }
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}