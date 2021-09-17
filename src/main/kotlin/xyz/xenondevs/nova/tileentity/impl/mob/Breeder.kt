package xyz.xenondevs.nova.tileentity.impl.mob

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Animals
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.BREEDER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
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
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.FoodUtils
import xyz.xenondevs.nova.util.item.canBredNow
import xyz.xenondevs.nova.util.item.genericMaxHealth
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.math.min

private val MAX_ENERGY = NovaConfig[BREEDER].getInt("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[BREEDER].getInt("energy_per_tick")!!
private val ENERGY_PER_BREED = NovaConfig[BREEDER].getInt("energy_per_breed")!!
private val IDLE_TIME = NovaConfig[BREEDER].getInt("idle_time")!!
private val BREED_LIMIT = NovaConfig[BREEDER].getInt("breed_limit")!!
private val MIN_RANGE = NovaConfig[BREEDER].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[BREEDER].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[BREEDER].getInt("range.default")!!

class Breeder(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 9, ::handleInventoryUpdate)
    override val gui = lazy { MobCrusherGUI() }
    override val upgradeHolder = UpgradeHolder(data, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_BREED, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val itemHolder = NovaItemHolder(this, inventory)
    
    private lateinit var region: Region
    
    private var timePassed = 0
    private var maxIdleTime = 0
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        region = getBlockFrontRegion(range, range, 4, -1)
        VisualRegion.updateRegion(uuid, region)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (timePassed++ >= maxIdleTime) {
                timePassed = 0
                
                val breedableEntities =
                    location
                        .chunk
                        .getSurroundingChunks(1, includeCurrent = true, ignoreUnloaded = true)
                        .flatMap { it.entities.asList() }
                        .filterIsInstance<Animals>()
                        .filter { it.canBredNow && it.location in region }
                
                var breedsLeft = min(energyHolder.energy / energyHolder.specialEnergyConsumption, BREED_LIMIT)
                for (animal in breedableEntities) {
                    val success = if (FoodUtils.requiresHealing(animal)) tryHeal(animal)
                    else tryBreed(animal)
                    
                    if (success) {
                        breedsLeft--
                        energyHolder.energy -= energyHolder.specialEnergyConsumption
                        if (breedsLeft == 0) break
                    }
                }
            }
        }
        
        if (gui.isInitialized()) gui.value.idleBar.percentage = timePassed / maxIdleTime.toDouble()
    }
    
    private fun tryHeal(animal: Animals): Boolean {
        for ((index, item) in inventory.items.withIndex()) {
            if (item == null) continue
            
            val healAmount = FoodUtils.getHealAmount(animal, item.type)
            if (healAmount > 0) {
                animal.health = min(animal.health + healAmount, animal.genericMaxHealth)
                inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                val remains = FoodUtils.getItemRemains(item.type)
                if (remains != null)
                    inventory.setItemStack(SELF_UPDATE_REASON, index, ItemStack(remains))
                
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
                inventory.addItemAmount(SELF_UPDATE_REASON, index, -1)
                
                val remains = FoodUtils.getItemRemains(item.type)
                if (remains != null)
                    inventory.setItemStack(SELF_UPDATE_REASON, index, ItemStack(remains))
                
                return true
            }
        }
        
        return false
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.updateReason != SELF_UPDATE_REASON && !event.isRemove && !FoodUtils.isFood(event.newItemStack.type))
            event.isCancelled = true
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    inner class MobCrusherGUI : TileEntityGUI("menu.nova.breeder") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Breeder,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(itemHolder.getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s p i i i . . |" +
                "| r n i i i . . |" +
                "| u m i i i . . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('r', VisualizeRegionItem(uuid) { region })
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        
        val idleBar = object : VerticalBar(gui, x = 6, y = 1, height = 3, NovaMaterialRegistry.GREEN_BAR) {
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.breeder.idle", maxIdleTime - timePassed))
        }
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
    }
    
}