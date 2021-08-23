package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BoneMealItem
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.blockPos
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.isFullyAged
import xyz.xenondevs.nova.util.nmsStack
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("fertilizer.capacity")!!
private val ENERGY_PER_FERTILIZE = NovaConfig.getInt("fertilizer.energy_per_fertilize")!!
private val WAIT_TIME = NovaConfig.getInt("fertilizer.wait_time")!!

class Fertilizer(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val fertilizerInventory = getInventory("fertilizer", 12, true, ::handleFertilizerUpdate)
    override val gui by lazy(::FertilizerGUI)
    
    private val fertilizeRegion = getFrontArea(7.0, 7.0, 1.0, 0.0)
    private var idleTime = WAIT_TIME
    
    init {
        setDefaultInventory(fertilizerInventory)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_FERTILIZE) {
            if (--idleTime <= 0) {
                idleTime = WAIT_TIME
                if (!fertilizerInventory.isEmpty)
                    fertilizeNextPlant()
            }
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
        }
    }
    
    private fun fertilizeNextPlant() {
        for ((index, item) in fertilizerInventory.items.withIndex()) {
            if (item == null) continue
            val plant = getRandomPlant() ?: return
            
            val context = UseOnContext(
                plant.world.serverLevel,
                null,
                InteractionHand.MAIN_HAND,
                item.nmsStack,
                BlockHitResult(Vec3.ZERO, Direction.DOWN, plant.location.blockPos, false)
            )
            BoneMealItem.applyBonemeal(context)
            
            energy -= ENERGY_PER_FERTILIZE
            fertilizerInventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
            break
        }
    }
    
    private fun getRandomPlant(): Block? =
        fertilizeRegion.blocks
            .filter {
                ProtectionManager.canUse(ownerUUID, it.location)
                    && ((it.blockData is Ageable && !it.isFullyAged()) || (it.blockData !is Ageable && it.type in PlantUtils.PLANTS))
            }
            .randomOrNull()
    
    private fun handleFertilizerUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && event.newItemStack.type != Material.BONE_MEAL)
            event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class FertilizerGUI : TileEntityGUI("menu.nova.fertilizer") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Fertilizer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                Triple(getNetworkedInventory(fertilizerInventory), "inventory.nova.fertilizer", ItemConnectionType.ALL_TYPES)
            ),
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s . . . . # . |" +
                "| v . . . . # . |" +
                "| u . . . . # . |" +
                "3 - - - - - - - 4")
            .addIngredient('v', VisualizeRegionItem(uuid, fertilizeRegion))
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', UpgradesTeaserItem)
            .build()
            .also { it.fillRectangle(2, 1, 4, fertilizerInventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
    }
    
}
