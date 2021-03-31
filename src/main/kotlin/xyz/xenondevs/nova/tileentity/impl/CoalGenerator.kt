package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.resourcepack.Icon
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.util.*
import java.util.*
import kotlin.math.min

private const val MAX_ENERGY = 10_000
private const val ENERGY_PER_TICK = 5

class CoalGenerator(material: NovaMaterial, uuid: UUID, armorStand: ArmorStand) : TileEntity(material, uuid, armorStand) {
    
    private var energy: Int = retrieveData(0, "energy")
    private var burnTime: Int = retrieveData(0, "burnTime")
    
    private val inventory = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed("inventory"), 1)
        .also { it.setItemUpdateHandler(this::handleInventoryUpdate) }
    private val gui by lazy { CoalGeneratorGUI() }
    
    override val drops: MutableList<ItemStack>
        get() {
            val drops = super.drops
            val fuel = inventory.getItemStack(0)
            if (fuel != null) drops += fuel
            return drops
        }
    
    override fun handleTick() {
        if (burnTime != 0) {
            burnTime--
            energy = min(MAX_ENERGY, energy + ENERGY_PER_TICK)
        } else burnItem()
        
        gui.energyItem.notifyWindows()
    }
    
    private fun burnItem() {
        val fuelStack = inventory.getItemStack(0)
        if (fuelStack != null) {
            val fuel = fuelStack.type.fuel!!
            burnTime += fuel.burnTime
            if (fuel.remains == null) {
                inventory.removeOne(null, 0)
            } else {
                inventory.setItemStack(null, 0, fuel.remains.toItemStack())
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.player != null) { // done by a player
            if (event.newItemStack != null && event.newItemStack.type.fuel == null) {
                // illegal item
                event.isCancelled = true
            }
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        gui.openWindow(event.player)
    }
    
    override fun handleDisable() {
        storeData("energy", energy)
        storeData("burnTime", burnTime)
    }
    
    override fun handleRemove() {
        VirtualInventoryManager.getInstance().remove(inventory)
        saveFile.delete()
    }
    
    inner class CoalGeneratorGUI {
        
        val progressItem = ProgressItem()
        val energyItem = EnergyItem()
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                "e # # # # # # # #" +
                "# # i # > # # # #" +
                "# # # # # # # # #")
            .addIngredient('#', Icon.BACKGROUND.item)
            .addIngredient('i', SlotElement.VISlotElement(inventory, 0))
            .addIngredient('e', energyItem)
            .addIngredient('>', progressItem)
            .build()
        
        init {
            runTaskTimer(0, 10) {
                if (progressItem.state + 1 > 16) progressItem.state = 0
                else progressItem.state++
            }
        }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Coal Generator", gui).show()
        }
        
    }
    
    inner class EnergyItem : BaseItem() {
        
        override fun getItemBuilder(): ItemBuilder {
            return Icon.LIGHT_BOX.itemBuilder.setDisplayName("§rEnergy: §b$energy").addLoreLines("§rBurn time left: $burnTime")
        }
        
        override fun handleClick(p0: ClickType?, p1: Player?, p2: InventoryClickEvent?) = Unit
        
    }
}
