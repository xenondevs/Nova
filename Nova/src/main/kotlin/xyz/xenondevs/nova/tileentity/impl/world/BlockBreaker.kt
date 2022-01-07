package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.Material
import xyz.xenondevs.nova.api.event.tileentity.TileEntityBreakBlockEvent
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BLOCK_BREAKER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

private val MAX_ENERGY = NovaConfig[BLOCK_BREAKER].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[BLOCK_BREAKER].getLong("energy_per_tick")!!
private val BREAK_SPEED_MULTIPLIER = NovaConfig[BLOCK_BREAKER].getDouble("break_speed_multiplier")!!
private val BREAK_SPEED_CLAMP = NovaConfig[BLOCK_BREAKER].getDouble("break_speed_clamp")!!
private val DROP_EXCESS_ON_GROUND = DEFAULT_CONFIG.getBoolean("drop_excess_on_ground")
private val DISABLE_BLOCK_BREAK_EFFECTS = DEFAULT_CONFIG.getBoolean("disable_block_break_effects")

class BlockBreaker(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 9) { if (it.isAdd && it.updateReason != SELF_UPDATE_REASON) it.isCancelled = true }
    override val gui = lazy { BlockBreakerGUI() }
    override val upgradeHolder = UpgradeHolder(this, gui, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT)
    
    private val entityId = uuid.hashCode()
    private val block = location.clone().advance(getFace(BlockSide.FRONT)).block
    private var lastType: Material? = null
    private var breakProgress = retrieveData("breakProgress") { 0.0 }
    
    override fun saveData() {
        super.saveData()
        storeData("breakProgress", breakProgress)
    }
    
    override fun handleTick() {
        val type = block.type
        if (energyHolder.energy >= ENERGY_PER_TICK
            && !type.isTraversable()
            && type.isBreakable()
            && ProtectionManager.canBreak(this, block.location)
            && (!DROP_EXCESS_ON_GROUND && !inventory.isFull())
        ) {
            // consume energy
            energyHolder.energy -= energyHolder.energyConsumption
            
            // reset progress when block changed
            if (lastType != null && type != lastType) breakProgress = 0.0
            
            // set last known type
            lastType = type
            
            // add progress
            val additionalProgress = min(BREAK_SPEED_CLAMP, block.type.breakSpeed * BREAK_SPEED_MULTIPLIER * upgradeHolder.getSpeedModifier())
            breakProgress += additionalProgress
            
            if (breakProgress >= 1.0) {
                var drops = block.getAllDrops().toMutableList()
                val event = TileEntityBreakBlockEvent(this, block, drops)
                callEvent(event)
                drops = event.drops
                
                // break block, add items to inventory / drop them if full
                block.breakAndTakeDrops(null, !DISABLE_BLOCK_BREAK_EFFECTS)
                drops.forEach { drop ->
                    val amountLeft = inventory.addItem(SELF_UPDATE_REASON, drop)
                    if (DROP_EXCESS_ON_GROUND && amountLeft != 0) {
                        drop.amount = amountLeft
                        world.dropItemNaturally(block.location.center(), drop)
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
    }
    
    inner class BlockBreakerGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@BlockBreaker,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Pair(itemHolder.getNetworkedInventory(inventory), "inventory.nova.default"))
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # i i i # e |" +
                "| u # i i i # e |" +
                "| # # i i i # e |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
    }
    
}