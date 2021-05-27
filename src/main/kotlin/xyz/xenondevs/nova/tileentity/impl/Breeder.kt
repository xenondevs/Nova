package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Location
import org.bukkit.entity.Animals
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.*
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = NovaConfig.getInt("breeder.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("breeder.energy_per_tick")!!
private val ENERGY_PER_BREED = NovaConfig.getInt("breeder.energy_per_breed")!!
private val IDLE_TIME = NovaConfig.getInt("breeder.idle_time")!!
private val BREED_LIMIT = NovaConfig.getInt("breeder.breed_limit")!!

class Breeder(
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
    
    private val inventory = getInventory("inventory", 9, true, ::handleInventoryUpdate)
    
    init {
        val frontFace = getFace(BlockSide.FRONT)
        val startLocation = location.clone().advance(frontFace)
        val pos1 = startLocation.clone().advance(getFace(BlockSide.LEFT), 3.0).apply { y -= 1 }
        val pos2 = startLocation.clone().advance(getFace(BlockSide.RIGHT), 3.0).advance(frontFace, 6.0).apply { y += 3 }
        val sorted = LocationUtils.sort(pos1, pos2)
        min = sorted.first
        max = sorted.second
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            energy -= ENERGY_PER_TICK
            
            if (--idleTime == 0) {
                idleTime = IDLE_TIME
                
                val breedableEntities =
                    location
                        .chunk
                        .getSurroundingChunks(1, includeCurrent = true, ignoreUnloaded = true)
                        .flatMap { it.entities.asList() }
                        .filterIsInstance<Animals>()
                        .filter { it.canBredNow && it.location.isBetween(min, max) }
                
                var breedsLeft = min(energy / ENERGY_PER_BREED, BREED_LIMIT)
                for (animal in breedableEntities) {
                    val success = if (FoodUtils.requiresHealing(animal)) tryHeal(animal)
                    else tryBreed(animal)
                    
                    if (success) {
                        breedsLeft--
                        energy -= ENERGY_PER_BREED
                        if (breedsLeft == 0) break
                    }
                }
            }
        }
        
        gui.idleBar.percentage = (IDLE_TIME - idleTime) / IDLE_TIME.toDouble()
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    private fun tryHeal(animal: Animals): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            val healAmount = FoodUtils.getHealAmount(animal, item.type)
            if (healAmount > 0) {
                animal.health = min(animal.health + healAmount, animal.genericMaxHealth)
                inventory.removeOne(SELF_UPDATE_REASON, index)
                return true
            }
        }
        
        return false
    }
    
    private fun tryBreed(animal: Animals): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            if (FoodUtils.canUseBreedFood(animal, item.type)) {
                animal.loveModeTicks = 600
                inventory.removeOne(SELF_UPDATE_REASON, index)
                return true
            }
        }
        
        return false
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd && !FoodUtils.isFood(event.newItemStack.type)) event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGUI : TileEntityGUI("Breeder") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Breeder,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            null
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . . . . . |" +
                "| r # . . . . . |" +
                "| # # . . . . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid, min, max))
            .build()
            .also { it.fillRectangle(3, 1, 3, inventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -ENERGY_PER_TICK) }
        
        val idleBar = object : VerticalBar(gui, x = 6, y = 1, height = 3, NovaMaterial.GREEN_BAR) {
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName("§7Next try in $idleTime ticks")
            
        }
        
    }
    
}