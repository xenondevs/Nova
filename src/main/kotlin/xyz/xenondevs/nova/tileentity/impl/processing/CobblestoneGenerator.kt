package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
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
import xyz.xenondevs.nova.material.NovaMaterialRegistry.COBBLESTONE_GENERATOR
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
import xyz.xenondevs.nova.util.MathUtils
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val ENERGY_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("energy_capacity")!!
private val ENERGY_PER_TICK = NovaConfig[COBBLESTONE_GENERATOR].getLong("energy_per_tick")!!
private val WATER_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("water_capacity")!!
private val LAVA_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("lava_capacity")!!
private val PROGRESS_PER_TICK = NovaConfig[COBBLESTONE_GENERATOR].getDouble("progress_per_tick")!!

class CobblestoneGenerator(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::CobblestoneGeneratorGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.FLUID)
    
    private val inventory = getInventory("inventory", 3, ::handleInventoryUpdate)
    private val waterTank = getFluidContainer("water", setOf(FluidType.WATER), WATER_CAPACITY, 0, ::updateHeadStack, upgradeHolder)
    private val lavaTank = getFluidContainer("lava", setOf(FluidType.LAVA), LAVA_CAPACITY, 0, ::updateHeadStack, upgradeHolder)
    
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    override val fluidHolder = NovaFluidHolder(this, waterTank to NetworkConnectionType.BUFFER, lavaTank to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var mode = retrieveEnum("mode") { GenerationMode.COBBLESTONE }
    
    private var progress = 0.0
    private var progressPerTick = 0.0
    
    init {
        handleUpgradeUpdates()
    }
    
    override fun getHeadStack() =
        material.block!!.createItemStack(
            MathUtils.convertBooleanArrayToInt(booleanArrayOf(!lavaTank.isEmpty(), !waterTank.isEmpty()))
        )
    
    private fun handleUpgradeUpdates() {
        progressPerTick = PROGRESS_PER_TICK * upgradeHolder.getSpeedModifier()
    }
    
    override fun handleTick() {
        if (waterTank.amount >= 1000
            && lavaTank.amount >= 1000
            && energyHolder.energy >= energyHolder.energyConsumption
            && inventory.canHold(mode.product)
        ) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            progress += progressPerTick
            if (progress >= 1.0) {
                progress = 0.0
                
                waterTank.takeFluid(mode.waterTake)
                lavaTank.takeFluid(mode.lavaTake)
                
                inventory.addItem(SELF_UPDATE_REASON, mode.product)
            }
            
            if (gui.isInitialized()) gui.value.progressItem.percentage = progress
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled == !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    override fun saveData() {
        super.saveData()
        storeData("mode", mode)
    }
    
    inner class CobblestoneGeneratorGUI : TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@CobblestoneGenerator,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output"),
            listOf(waterTank to "container.nova.water_tank", lavaTank to "container.nova.lava_tank"),
            openPrevious = ::openWindow
        )
        
        val progressItem = ProgressArrowItem()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| w l # i # s e |" +
                "| w l > i # u e |" +
                "| w l # i # m e |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('m', ChangeModeItem())
            .addIngredient('i', inventory)
            .addIngredient('>', progressItem)
            .build()
        
        init {
            FluidBar(gui, x = 1, y = 1, height = 3, waterTank)
            FluidBar(gui, x = 2, y = 1, height = 3, lavaTank)
            EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        }
        
        private inner class ChangeModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return mode.uiItem.itemProvider
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                if (clickType == ClickType.LEFT || clickType == ClickType.RIGHT) {
                    val direction = if (clickType == ClickType.LEFT) 1 else -1
                    mode = GenerationMode.values()[(mode.ordinal + direction).mod(GenerationMode.values().size)]
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    notifyWindows()
                }
            }
            
        }
        
    }
    
}

private enum class GenerationMode(val waterTake: Long, val lavaTake: Long, val product: ItemStack, val uiItem: NovaMaterial) {
    
    COBBLESTONE(0L, 0L, ItemStack(Material.COBBLESTONE), NovaMaterialRegistry.COBBLESTONE_MODE_BUTTON),
    STONE(1000L, 0L, ItemStack(Material.STONE), NovaMaterialRegistry.STONE_MODE_BUTTON),
    OBSIDIAN(0L, 1000L, ItemStack(Material.OBSIDIAN), NovaMaterialRegistry.OBSIDIAN_MODE_BUTTON)
    
}