package xyz.xenondevs.nova.tileentity.impl.world

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig.getInt("block_breaker.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("block_breaker.energy_per_tick")!!
private val BREAK_SPEED_MULTIPLIER = NovaConfig.getDouble("block_breaker.break_speed_multiplier")!!
private val BREAK_SPEED_CLAMP = NovaConfig.getDouble("block_breaker.break_speed_clamp")!!

class BlockBreaker(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 9, true) { if (it.isAdd && it.updateReason != SELF_UPDATE_REASON) it.isCancelled = true }
    private val entityId = uuid.hashCode()
    private val block = location.clone().advance(getFace(BlockSide.FRONT)).block
    private var lastType: Material? = null
    private var breakProgress = retrieveData("breakProgress") { 0.0 }
    
    override val gui by lazy { BlockBreakerGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("breakProgress", breakProgress)
    }
    
    override fun handleTick() {
        val type = block.type
        if (energy >= ENERGY_PER_TICK
            && !type.isTraversable()
            && type.isBreakable()
            && ProtectionUtils.canBreak(ownerUUID, block.location)
        ) {
            // consume energy
            energy -= ENERGY_PER_TICK
            
            // reset progress when block changed
            if (lastType != null && type != lastType) breakProgress = 0.0
            
            // set last known type
            lastType = type
            
            // add progress
            val additionalProgress = min(BREAK_SPEED_CLAMP, block.type.breakSpeed * BREAK_SPEED_MULTIPLIER)
            breakProgress += additionalProgress
            
            if (breakProgress >= 1.0) {
                // break block, add items to inventory / drop them if full
                val drops = block.breakAndTakeDrops()
                drops.forEach { drop ->
                    val amountLeft = inventory.addItem(SELF_UPDATE_REASON, drop)
                    if (amountLeft != 0) {
                        drop.amount = amountLeft
                        world.dropItemNaturally(location, drop)
                    }
                }
                
                // reset break progress
                breakProgress = 0.0
    
                block.setBreakState(entityId, -1)
            } else {
                // send break state
                block.setBreakState(entityId, (breakProgress * 9).roundToInt())
            }
            
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    inner class BlockBreakerGUI : TileEntityGUI("menu.nova.block_breaker") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@BlockBreaker,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.EXTRACT_TYPES))
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . . . # . |" +
                "| # # . . . # . |" +
                "| # # . . . # . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
            .also { it.fillRectangle(3, 1, 3, inventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -ENERGY_PER_TICK) }
        
    }
    
}