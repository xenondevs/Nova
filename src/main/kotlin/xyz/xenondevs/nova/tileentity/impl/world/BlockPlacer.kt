package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import org.bukkit.Material
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.other.ItemsAdder
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.*
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("block_placer.capacity")!!
private val ENERGY_PER_PLACE = NovaConfig.getInt("block_placer.energy_per_place")!!

class BlockPlacer(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 9, true) { }
    private val placeLocation = location.clone().advance(getFace(BlockSide.FRONT))
    private val placeBlock = location.clone().advance(getFace(BlockSide.FRONT)).block
    
    override val gui by lazy { BlockPlacerGUI() }
    
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
                    if (TileEntityLimits.canPlaceTileEntity(ownerUUID, world, novaMaterial) == PlaceResult.ALLOW) {
                        TileEntityManager.placeTileEntity(ownerUUID, placeBlock.location, armorStand.location.yaw, novaMaterial, null)
                        novaMaterial.hitboxType?.playPlaceSoundEffect(placeBlock.location)
                    } else continue
                } else {
                    placeBlock.place(item)
                    material.playPlaceSoundEffect(placeBlock.location)
                }
                
                inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                return true
            } else if (ItemsAdder.isInstalled()) {
                if (ItemsAdder.placeItem(item, placeBlock.location)) {
                    inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                    return true
                }
            }
        }
        
        return false
    }
    
    override fun handleTick() {
        val type = placeBlock.type
        if (energy >= ENERGY_PER_PLACE
            && type == Material.AIR
            && TileEntityManager.getTileEntityAt(placeLocation) == null
            && ProtectionManager.canPlace(ownerUUID, placeBlock.location)
        ) {
            if (placeBlock()) energy -= ENERGY_PER_PLACE
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    inner class BlockPlacerGUI : TileEntityGUI("menu.nova.block_placer") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@BlockPlacer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
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
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -1) }
        
    }
    
}