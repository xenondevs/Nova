package xyz.xenondevs.nova.tileentity.impl

import com.google.gson.JsonObject
import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Farmland
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.region.Region
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.isEmpty
import xyz.xenondevs.nova.util.item.PlantUtils
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.isHoe
import xyz.xenondevs.nova.util.item.isTillable
import xyz.xenondevs.nova.util.protection.ProtectionUtils
import xyz.xenondevs.nova.util.soundGroup
import java.util.*
import kotlin.random.Random

private val MAX_ENERGY = NovaConfig.getInt("planter.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("planter.energy_per_tick")!!
private val WAIT_TIME = NovaConfig.getInt("planter.wait_time")!!

class Planter(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, data, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inputInventory = getInventory("input", 6, true, ::handleSeedUpdate)
    private val hoesInventory = getInventory("hoes", 1, true, ::handleHoeUpdate)
    override val gui: TileEntityGUI by lazy(::PlanterGUI)
    
    private val plantRegion = getFrontArea(7.0, 7.0, 1.0, 0.0)
    private val soilRegion = Region(plantRegion.min.clone().advance(BlockFace.DOWN), plantRegion.max.clone().advance(BlockFace.DOWN))
    
    private var autoHoe = retrieveData("autoHoe") { true }
    private var nextSeed = WAIT_TIME
    
    init {
        addAvailableInventories(inputInventory, hoesInventory)
        setDefaultInventory(inputInventory)
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            if (nextSeed > 0) nextSeed--
            else {
                nextSeed = WAIT_TIME
                placeNextSeed()
            }
        }
    }
    
    private fun placeNextSeed() {
        if(hoesInventory.items[0] == null)
            return
        val (plant, soil) = getNextBlock() ?: return
        energy -= ENERGY_PER_TICK
        if (autoHoe && soil.type != Material.FARMLAND) tillDirt(soil)
        val item = takeSeed() ?: return
        plant.type = PlantUtils.SEED_BLOCKS[item.type] ?: item.type
        plant.world.playSound(plant.location, plant.type.soundGroup.placeSound, 1f, Random.nextDouble(0.6, 1.0).toFloat())
        useHoe()
    }
    
    private fun takeSeed(): ItemStack? {
        val index = inputInventory.items.indexOfFirst { it != null }
        if (index == -1)
            return null
        val item = inputInventory.getItemStack(index)
        inputInventory.addItemAmount(null, index, -1)
        return item
    }
    
    private fun getNextBlock(): Pair<Block, Block>? {
        val emptyInput = inputInventory.isEmpty()
        val index = plantRegion.withIndex().indexOfFirst { (index, block) ->
            // Search for a block that has no block on top of it and is dirt/farmland
            val soilBlock = soilRegion[index]
            
            if (!ProtectionUtils.canPlace(ownerUUID, block.location) || !ProtectionUtils.canBreak(ownerUUID, soilBlock.location))
                return@indexOfFirst false
            
            if (emptyInput)
                return@indexOfFirst block.type == Material.AIR && soilBlock.type != Material.FARMLAND && autoHoe && soilBlock.type.isTillable()
            else
                return@indexOfFirst block.type == Material.AIR && (soilBlock.type == Material.FARMLAND || autoHoe && soilBlock.type.isTillable())
        }
        if (index == -1)
            return null
        return plantRegion[index] to soilRegion[index]
    }
    
    private fun tillDirt(block: Block) {
        block.type = Material.FARMLAND
        val farmland = block.blockData as Farmland
        farmland.moisture = 7
        world.playSound(block.location, Sound.ITEM_HOE_TILL, 1f, 1f)
        useHoe()
    }
    
    private fun handleHoeUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && !event.newItemStack.type.isHoe())
            event.isCancelled = true
    }
    
    private fun handleSeedUpdate(event: ItemUpdateEvent) {
        if ((event.isAdd || event.isSwap) && event.newItemStack.type !in PlantUtils.PLANTS)
            event.isCancelled = true
    }
    
    private fun useHoe() {
        hoesInventory.setItemStack(null, 0, ToolUtils.damageTool(hoesInventory.items[0]))
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("autoHoe", autoHoe)
    }
    
    inner class PlanterGUI : TileEntityGUI("menu.nova.planter") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Planter,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                Triple(getNetworkedInventory(inputInventory), "inventory.nova.input", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(hoesInventory), "inventory.nova.hoes", ItemConnectionType.INSERT_TYPES)
            ),
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s a f # # # . |" +
                "| . . . # h # . |" +
                "| . . . # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('h', SlotElement.VISlotElement(hoesInventory, 0))
            .addIngredient('a', VisualizeRegionItem(uuid, plantRegion))
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', AutoHoeingItem())
            .build()
            .also { it.fillRectangle(1, 2, 3, inputInventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -ENERGY_PER_TICK) }
        
        private inner class AutoHoeingItem : BaseItem() {
            
            override fun getItemBuilder(): ItemBuilder {
                return if (autoHoe)
                    NovaMaterial.HOE_ON_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.planter.autohoe.on")
                else NovaMaterial.HOE_OFF_BUTTON.createBasicItemBuilder().setLocalizedName("menu.nova.planter.autohoe.off")
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                autoHoe = !autoHoe
                notifyWindows()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
}