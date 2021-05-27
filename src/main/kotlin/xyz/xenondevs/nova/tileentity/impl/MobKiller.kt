package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Mob
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.*
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = NovaConfig.getInt("mob_killer.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("mob_killer.energy_per_tick")!!
private val ENERGY_PER_DAMAGE = NovaConfig.getInt("mob_killer.energy_per_damage")!!
private val IDLE_TIME = NovaConfig.getInt("mob_killer.idle_time")!!
private val KILL_LIMIT = NovaConfig.getInt("mob_killer.kill_limit")!!
private val DAMAGE = NovaConfig.getDouble("mob_killer.damage")!!

class MobKiller(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val gui by lazy { MobCrusherGUI() }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private var idleTime = IDLE_TIME
    private var min: Location
    private var max: Location
    
    init {
        val frontFace = getFace(BlockSide.FRONT)
        val startLocation = location.clone().advance(frontFace)
        val pos1 = startLocation.clone().advance(getFace(BlockSide.LEFT), 5.0).apply { y -= 1 }
        val pos2 = startLocation.clone().advance(getFace(BlockSide.RIGHT), 5.0).advance(frontFace, 10.0).apply { y += 3 }
        val sorted = LocationUtils.sort(pos1, pos2)
        min = sorted.first
        max = sorted.second
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            energy -= ENERGY_PER_TICK
            
            if (--idleTime == 0) {
                idleTime = IDLE_TIME
                
                val killLimit = min(energy / ENERGY_PER_DAMAGE, KILL_LIMIT)
                
                location
                    .chunk
                    .getSurroundingChunks(1, includeCurrent = true, ignoreUnloaded = true)
                    .flatMap { it.entities.asList() }
                    .filterIsInstance<Mob>()
                    .filter { it.location.isBetween(min, max) }
                    .take(killLimit)
                    .forEach { entity ->
                        energy -= ENERGY_PER_DAMAGE
                        entity.damage(DAMAGE)
                    }
            }
        }
        
        gui.idleBar.percentage = (IDLE_TIME - idleTime) / IDLE_TIME.toDouble()
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGUI : TileEntityGUI("Mob Killer") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MobKiller,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            null
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . # . # # |" +
                "| r # . # . # # |" +
                "| # # . # . # # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid, min, max))
            .build()
        
        val energyBar = EnergyBar(gui, x = 3, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -ENERGY_PER_TICK) }
        
        val idleBar = object : VerticalBar(gui, x = 5, y = 1, height = 3, NovaMaterial.GREEN_BAR) {
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName("§7Next attack in $idleTime ticks")
            
        }
        
    }
    
}