package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.minecraft.world.entity.EquipmentSlot
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
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
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
import xyz.xenondevs.nova.ui.item.LeftRightFluidProgressItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

private const val MAX_STATE = 99

private val ENERGY_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("energy_capacity")!!
private val ENERGY_PER_TICK = NovaConfig[COBBLESTONE_GENERATOR].getLong("energy_per_tick")!!
private val WATER_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("water_capacity")!!
private val LAVA_CAPACITY = NovaConfig[COBBLESTONE_GENERATOR].getLong("lava_capacity")!!
private val MB_PER_TICK = NovaConfig[COBBLESTONE_GENERATOR].getLong("mb_per_tick")!!

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
    private val waterTank = getFluidContainer("water", setOf(FluidType.WATER), WATER_CAPACITY, 0, ::updateWaterLevel, upgradeHolder)
    private val lavaTank = getFluidContainer("lava", setOf(FluidType.LAVA), LAVA_CAPACITY, 0, ::updateLavaLevel, upgradeHolder)
    
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT) { createSideConfig(NetworkConnectionType.EXTRACT, BlockSide.FRONT) }
    override val fluidHolder = NovaFluidHolder(this, waterTank to NetworkConnectionType.BUFFER, lavaTank to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var mode = retrieveEnum("mode") { GenerationMode.COBBLESTONE }
    private var mbPerTick = 0L
    
    private var currentMode = mode
    private var mbUsed = 0L
    
    private val waterLevel = FakeArmorStand(armorStand.location) { it.isInvisible = true;it.isMarker = true }
    private val lavaLevel = FakeArmorStand(armorStand.location) { it.isInvisible = true; it.isMarker = true }
    
    private val particleEffect = particleBuilder(ParticleEffect.SMOKE_LARGE) {
        location(armorStand.location.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
        offset(getFace(BlockSide.RIGHT).axis, 0.15f)
        amount(5)
        speed(0.03f)
    }
    
    init {
        handleUpgradeUpdates()
        updateWaterLevel()
        updateLavaLevel()
    }
    
    private fun handleUpgradeUpdates() {
        mbPerTick = (MB_PER_TICK * upgradeHolder.getSpeedModifier()).roundToLong()
    }
    
    private fun updateWaterLevel() {
        val item = if (!waterTank.isEmpty()) {
            val state = getFluidState(waterTank)
            NovaMaterialRegistry.COBBLESTONE_GENERATOR_WATER_LEVELS.item.createItemStack(state)
        } else null
        waterLevel.setEquipment(EquipmentSlot.HEAD, item)
        waterLevel.updateEquipment()
    }
    
    private fun updateLavaLevel() {
        val item = if (!lavaTank.isEmpty()) {
            val state = getFluidState(lavaTank)
            NovaMaterialRegistry.COBBLESTONE_GENERATOR_LAVA_LEVELS.item.createItemStack(state)
        } else null
        lavaLevel.setEquipment(EquipmentSlot.HEAD, item)
        lavaLevel.updateEquipment()
    }
    
    private fun getFluidState(container: FluidContainer) =
        (container.amount.toDouble() / container.capacity.toDouble() * MAX_STATE.toDouble()).roundToInt()
    
    override fun handleTick() {
        val mbToTake = min(mbPerTick, 1000 - mbUsed)
        
        if (waterTank.amount >= mbToTake
            && lavaTank.amount >= mbToTake
            && energyHolder.energy >= energyHolder.energyConsumption
            && inventory.canHold(currentMode.product)
        ) {
            energyHolder.energy -= energyHolder.energyConsumption
            mbUsed += mbToTake
            
            when {
                currentMode.takeLava -> lavaTank
                currentMode.takeWater -> waterTank
                else -> null
            }?.takeFluid(mbToTake)
            
            if (mbUsed >= 1000) {
                mbUsed = 0
                inventory.addItem(SELF_UPDATE_REASON, currentMode.product)
                currentMode = mode
                
                playSoundEffect(Sound.BLOCK_LAVA_EXTINGUISH, 0.1f, Random.nextDouble(0.5, 1.95).toFloat())
                particleEffect.display(getViewers())
            }
            
            if (gui.isInitialized()) gui.value.progressItem.percentage = mbUsed / 1000.0
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        waterLevel.remove()
        lavaLevel.remove()
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
        
        val progressItem = LeftRightFluidProgressItem()
        
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
            FluidBar(gui, x = 1, y = 1, height = 3, fluidHolder, waterTank)
            FluidBar(gui, x = 2, y = 1, height = 3, fluidHolder, lavaTank)
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

private enum class GenerationMode(val takeWater: Boolean, val takeLava: Boolean, val product: ItemStack, val uiItem: NovaMaterial) {
    
    COBBLESTONE(false, false, ItemStack(Material.COBBLESTONE), NovaMaterialRegistry.COBBLESTONE_MODE_BUTTON),
    STONE(true, false, ItemStack(Material.STONE), NovaMaterialRegistry.STONE_MODE_BUTTON),
    OBSIDIAN(false, true, ItemStack(Material.OBSIDIAN), NovaMaterialRegistry.OBSIDIAN_MODE_BUTTON)
    
}