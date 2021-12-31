package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FREEZER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.lang.Long.min
import java.util.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val WATER_CAPACITY = NovaConfig[FREEZER].getLong("water_capacity")!!
private val ENERGY_CAPACITY = NovaConfig[FREEZER].getLong("energy_capacity")!!
private val ENERGY_PER_TICK = NovaConfig[FREEZER].getLong("energy_per_tick")!!
private val MB_PER_TICK = NovaConfig[FREEZER].getLong("mb_per_tick")!!

class Freezer(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::FreezerGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.FLUID)
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    private val waterTank = getFluidContainer("water", setOf(FluidType.WATER), WATER_CAPACITY, 0)
    
    override val fluidHolder = NovaFluidHolder(this, waterTank to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    
    private var mbPerTick = 0L
    private var mbUsed = 0L
    
    private var mode = retrieveEnum("mode") { FreezerMode.ICE }
    
    init {
        handleUpgradeUpdates()
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled == !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    private fun handleUpgradeUpdates() {
        mbPerTick = (MB_PER_TICK * upgradeHolder.getSpeedModifier()).roundToLong()
    }
    
    override fun handleTick() {
        val mbMaxPerOperation = 1000 * mode.maxCostMultiplier
        
        if (mbUsed > mbMaxPerOperation && inventory.canHold(mode.product)) {
            val compensationItems = ItemStack(Material.ICE, (mbUsed / 1000.0).roundToInt())
            if (inventory.canHold(compensationItems)) {
                inventory.addItem(SELF_UPDATE_REASON, compensationItems) // Add ice from overflowing water to the inventory
                mbUsed = (mbUsed / 1000) - floor(mbUsed / 1000.0).toLong() // Add rest to the used millibuckets
            }
        }
        val mbToTake = min(mbPerTick, mbMaxPerOperation - mbUsed)
        if (waterTank.amount >= mbToTake && energyHolder.energy >= energyHolder.energyConsumption && inventory.canHold(mode.product)) {
            energyHolder.energy -= energyHolder.energyConsumption
            mbUsed += mbToTake
            waterTank.takeFluid(mbToTake)
            if (mbUsed >= mbMaxPerOperation) {
                mbUsed = 0
                inventory.addItem(SELF_UPDATE_REASON, ItemStack(mode.product))
            }
            if (gui.isInitialized()) gui.value.progressItem.percentage = mbUsed / mbMaxPerOperation.toDouble()
        }
    }
    
    override fun saveData() {
        super.saveData()
        storeData("mode", mode)
    }
    
    inner class FreezerGUI : TileEntityGUI() {
        
        val progressItem = ProgressArrowItem()
        private val sideConfigGUI = SideConfigGUI(this@Freezer,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output"),
            listOf(waterTank to "container.nova.water_tank"),
            openPrevious = ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| w # # # # s e |" +
                "| w # > i # u e |" +
                "| w # # # # m e |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('>', SimpleItem(NovaMaterialRegistry.PROGRESS_ARROW.itemProvider))
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('m', ChangeModeItem())
            .build()
        
        init {
            FluidBar(gui, 1, 1, 3, fluidHolder, waterTank)
            EnergyBar(gui, 7, 1, 3, energyHolder)
        }
        
        private inner class ChangeModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return mode.uiItem.itemProvider
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                    val direction = if (clickType == ClickType.LEFT) 1 else -1
                    mode = FreezerMode.values()[(mode.ordinal + direction).mod(FreezerMode.values().size)]
                    
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    notifyWindows()
                }
            }
            
        }
    }
}

private enum class FreezerMode(val product: ItemStack, val uiItem: NovaMaterial, val maxCostMultiplier: Int) {
    ICE(ItemStack(Material.ICE), NovaMaterialRegistry.PINK_BUTTON, 1),
    PACKED_ICE(ItemStack(Material.PACKED_ICE), NovaMaterialRegistry.GREEN_BUTTON, 1),
    BLUE_ICE(ItemStack(Material.BLUE_ICE), NovaMaterialRegistry.BLUE_BUTTON, 5)
}
