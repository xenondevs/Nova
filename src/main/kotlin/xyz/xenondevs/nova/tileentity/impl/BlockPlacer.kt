package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.playPlaceSoundEffect
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("block_placer.capacity")!!
private val ENERGY_PER_PLACE = NovaConfig.getInt("block_placer.energy_per_place")!!

class BlockPlacer(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 9, true) { }
    private val block = location.clone().advance(getFace(BlockSide.FRONT)).block
    
    private val gui by lazy { BlockPlacerGUI() }
    
    init {
        setDefaultInventory(inventory)
    }
    
    private fun placeBlock(): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            val material = item.type
            if (material.isBlock) {
                val novaMaterial = item.novaMaterial
                if (novaMaterial != null && novaMaterial.isBlock) {
                    TileEntityManager.placeTileEntity(ownerUUID, block.location, armorStand.location.yaw, novaMaterial)
                    novaMaterial.hitbox?.playPlaceSoundEffect(block.location)
                } else {
                    block.type = material
                    material.playPlaceSoundEffect(block.location)
                }
                
                inventory.removeOne(SELF_UPDATE_REASON, index)
                return true
            }
        }
        
        return false
    }
    
    override fun handleTick() {
        val type = block.type
        if (energy >= ENERGY_PER_PLACE
            && type == Material.AIR
            && ProtectionUtils.canPlace(ownerUUID, block.location)
        ) {
            if (placeBlock()) energy -= ENERGY_PER_PLACE
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    
    private inner class BlockPlacerGUI {
        
        private val sideConfigGUI = SideConfigGUI(
            this@BlockPlacer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(inventory to "BlockPlacer Inventory")
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # . . . # . |" +
                "| # # . . . # . |" +
                "| # # . . . # . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
            .also { it.fillRectangle(3, 1, 3, inventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Block Placer", gui).show()
        }
        
    }
    
}