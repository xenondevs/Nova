package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.PUMP
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

private val ENERGY_CAPACITY = NovaConfig[PUMP].getLong("energy_capacity")!!
private val ENERGY_PER_TICK = NovaConfig[PUMP].getLong("energy_per_tick")!!
private val FLUID_CAPACITY = NovaConfig[PUMP].getLong("fluid_capacity")!!
private val REPLACEMENT_BLOCK = Material.valueOf(NovaConfig[PUMP].getString("replacement_block")!!)
private val IDLE_TIME = NovaConfig[PUMP].getLong("idle_time")!!

private val MIN_RANGE = NovaConfig[PUMP].getInt("range.min")!!
private val MAX_RANGE = NovaConfig[PUMP].getInt("range.max")!!
private val DEFAULT_RANGE = NovaConfig[PUMP].getInt("range.default")!!

class Pump(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::PumpGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates)
    
    private val fluidTank = getFluidContainer("tank", hashSetOf(FluidType.WATER, FluidType.LAVA), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder = upgradeHolder) { createExclusiveEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.TOP) }
    override val fluidHolder = NovaFluidHolder(this, fluidTank to NetworkConnectionType.EXTRACT, defaultConnectionConfig = { createExclusiveSideConfig(NetworkConnectionType.EXTRACT, BlockSide.TOP) })
    
    private var maxIdleTime = 0
    private var idleTime = 0
    
    private var mode = retrieveEnum("mode") { PumpMode.REPLACE }
    
    private var maxRange = 0
    private var range = retrieveData("range") { DEFAULT_RANGE }
        set(value) {
            field = value
            updateRegion()
            if (gui.isInitialized()) gui.value.updateRangeItems()
        }
    private lateinit var region: Region
    
    private var lastBlock: Block? = null
    private var sortedFaces = LinkedList(HORIZONTAL_FACES)
    
    init {
        handleUpgradeUpdates()
        updateRegion()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (idleTime > maxIdleTime) idleTime = maxIdleTime
        
        maxRange = MAX_RANGE + upgradeHolder.getRangeModifier()
        if (maxRange < range) range = maxRange
    }
    
    private fun updateRegion() {
        val rangeDouble = range.toDouble()
        val min = location.clone().subtract(rangeDouble - 1, rangeDouble, rangeDouble - 1)
        val max = location.clone().add(rangeDouble, 0.0, rangeDouble)
        region = Region(min, max)
        VisualRegion.updateRegion(uuid, region)
        idleTime = maxIdleTime
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && fluidTank.accepts(FluidType.WATER, 1000)) {
            if (--idleTime <= 0)
                pumpNextBlock()
        }
    }
    
    private fun pumpNextBlock() {
        val (block, type) = getNextBlock()
        if (block != null && type != null) {
            if (mode == PumpMode.REPLACE) {
                block.type = REPLACEMENT_BLOCK
                REPLACEMENT_BLOCK.playPlaceSoundEffect(block.location)
            } else if (!block.isInfiniteWaterSource()) {
                block.type = Material.AIR
            }
            fluidTank.addFluid(type, 1000)
            lastBlock = block
            energyHolder.energy -= energyHolder.energyConsumption
            idleTime = maxIdleTime
        } else {
            lastBlock = null
            idleTime = 60 * 20 // ByteZ' Idee
        }
    }
    
    private fun getNextBlock(): Pair<Block?, FluidType?> {
        var block: Block? = null
        var type: FluidType? = null
        if (lastBlock != null) {
            val pair = getRelativeBlock()
            block = pair.first
            type = pair.second
        }
        if (block == null) {
            val pair = searchBlock()
            block = pair.first
            type = pair.second
        }
        return block to type
    }
    
    private fun getRelativeBlock(): Pair<Block?, FluidType?> {
        val location = lastBlock!!.location
        val faces = VERTICAL_FACES + sortedFaces
        var block: Block? = null
        var type: FluidType? = null
        for (face in faces) {
            val newLocation = location.clone().advance(face, 1.0)
            val newBlock = newLocation.block
            
            val fluidType = newBlock.sourceFluidType ?: continue
            if (fluidTank.accepts(fluidType) && newLocation.center() in region && ProtectionManager.canBreak(ownerUUID, newBlock.location)) {
                if (face !in VERTICAL_FACES)
                    sortedFaces.rotateRight()
                block = newBlock
                type = fluidType
                break
            }
        }
        return block to type
    }
    
    private fun searchBlock(): Pair<Block?, FluidType?> {
        repeat(range) { r ->
            if (r == 0) {
                val block = location.clone().advance(BlockFace.DOWN).block
                val fluidType = block.sourceFluidType ?: return@repeat
                if (fluidTank.accepts(fluidType) && ProtectionManager.canBreak(ownerUUID, block.location))
                    return block to fluidType
                return@repeat
            }
            for (x in -r..r) {
                for (y in -r - 1..r) {
                    for (z in -r..r) {
                        if ((x != -r && x != r) && (y != -r - 1 && y != r) && (z != -r && z != r))
                            continue
                        val block = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                        val fluidType = block.sourceFluidType ?: continue
                        if (fluidTank.accepts(fluidType) && ProtectionManager.canBreak(ownerUUID, block.location))
                            return block to fluidType
                    }
                }
            }
        }
        return null to null
    }
    
    private fun Block.isInfiniteWaterSource(): Boolean {
        var waterCount = 0
        for (it in HORIZONTAL_FACES) {
            val newBlock = location.clone().advance(it, 1.0).block
            if ((newBlock.type == Material.WATER || newBlock.type == Material.BUBBLE_COLUMN) && newBlock.isSourceFluid())
                if (++waterCount > 1)
                    return true
        }
        return false
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        VisualRegion.removeRegion(uuid)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("range", range)
        storeData("mode", mode)
    }
    
    inner class PumpGUI : TileEntity.TileEntityGUI() {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Pump,
            fluidContainers = listOf(fluidTank to "container.nova.fluid_tank"),
            allowedEnergyTypes = listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            openPrevious = ::openWindow
        )
        
        private val rangeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s p # . # . M |" +
                "| u n # . # . # |" +
                "| v m # . # . # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('v', VisualizeRegionItem(uuid) { region })
            .addIngredient('p', AddNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range }.also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_RANGE..maxRange }, { range }, { range = it }).also(rangeItems::add))
            .addIngredient('M', PumpModeItem())
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('f', FluidBar(3, fluidHolder, fluidTank))
            .build()
        
        fun updateRangeItems() = rangeItems.forEach(Item::notifyWindows)
        
        private inner class PumpModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return if (mode == PumpMode.PUMP)
                    NovaMaterialRegistry.PUMP_PUMP_ICON.createBasicItemBuilder().setLocalizedName("menu.nova.pump.pump_mode")
                else
                    NovaMaterialRegistry.PUMP_REPLACE_ICON.createBasicItemBuilder().setLocalizedName("menu.nova.pump.replace_mode")
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                mode = if (mode == PumpMode.PUMP) PumpMode.REPLACE else PumpMode.PUMP
                notifyWindows()
            }
        }
        
    }
    
}

private enum class PumpMode {
    PUMP, // Replace fluid with air
    REPLACE // Replace fluid with block
}