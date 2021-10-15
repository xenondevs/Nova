package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Axis
import org.bukkit.Location
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
import xyz.xenondevs.nova.material.NovaMaterialRegistry.QUARRY
import xyz.xenondevs.nova.tileentity.*
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates

private val SCAFFOLDING_STACKS = NovaMaterialRegistry.SCAFFOLDING.block!!.let { modelData -> modelData.dataArray.indices.map { modelData.createItemStack(it) } }
private val FULL_HORIZONTAL = SCAFFOLDING_STACKS[0]
private val FULL_VERTICAL = SCAFFOLDING_STACKS[1]
private val CORNER_DOWN = SCAFFOLDING_STACKS[2]
private val SMALL_HORIZONTAL = SCAFFOLDING_STACKS[3]
private val FULL_SLIM_VERTICAL = SCAFFOLDING_STACKS[4]
private val SLIM_VERTICAL_DOWN = SCAFFOLDING_STACKS[5]
private val DRILL = NovaMaterialRegistry.NETHERITE_DRILL.createItemStack()

private val MIN_SIZE = NovaConfig[QUARRY].getInt("min_size")!!
private val MAX_SIZE = NovaConfig[QUARRY].getInt("max_size")!!
private val DEFAULT_SIZE_X = NovaConfig[QUARRY].getInt("default_size_x")!!
private val DEFAULT_SIZE_Z = NovaConfig[QUARRY].getInt("default_size_z")!!

private val MOVE_SPEED = NovaConfig[QUARRY].getDouble("move_speed")!!
private val DRILL_SPEED_MULTIPLIER = NovaConfig[QUARRY].getDouble("drill_speed_multiplier")!!
private val DRILL_SPEED_CLAMP = NovaConfig[QUARRY].getDouble("drill_speed_clamp")!!

private val MAX_ENERGY = NovaConfig[QUARRY].getInt("capacity")!!
private val BASE_ENERGY_CONSUMPTION = NovaConfig[QUARRY].getInt("base_energy_consumption")!!
private val ENERGY_PER_SQUARE_BLOCK = NovaConfig[QUARRY].getInt("energy_consumption_per_square_block")!!

class Quarry(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy { QuarryGUI() }
    private val inventory = getInventory("quarryInventory", 9) {}
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ENERGY_AND_RANGE)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, 0, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(this, inventory to ItemConnectionType.EXTRACT)
    
    private val entityId = uuid.hashCode()
    
    private var sizeX = retrieveData("sizeX") { DEFAULT_SIZE_X }
    private var sizeZ = retrieveData("sizeZ") { DEFAULT_SIZE_Z }
    
    private var energyPerTick by Delegates.notNull<Int>()
    
    private val solidScaffolding = createMultiModel()
    private val armX = createMultiModel()
    private val armZ = createMultiModel()
    private val armY = createMultiModel()
    private val drill = createMultiModel()
    
    private var maxSize = 0
    private var drillSpeedMultiplier = 0.0
    private var moveSpeed = 0.0
    
    private val y = location.blockY
    private var minX = 0
    private var minZ = 0
    private var maxX = 0
    private var maxZ = 0
    
    private var lastPointerLocation: Location
    private var pointerLocation: Location
    private var pointerDestination: Location? = retrieveOrNull("pointerDestination")
    
    private var drillProgress = retrieveData("drillProgress") { 0.0 }
    private var drilling = retrieveData("drilling") { false }
    private var done = retrieveData("done") { false }
    
    private val energySufficiency: Double
        get() = min(1.0, energyHolder.energy.toDouble() / energyPerTick.toDouble())
    
    private val currentMoveSpeed: Double
        get() = moveSpeed * energySufficiency
    
    private val currentDrillSpeedMultiplier: Double
        get() = drillSpeedMultiplier * energySufficiency
    
    init {
        handleUpgradeUpdates()
        updateBounds()
        
        pointerLocation = retrieveOrNull("pointerLocation") ?: Location(world, minX + 1.5, y - 2.0, minZ + 1.5)
        lastPointerLocation = retrieveOrNull("lastPointerLocation") ?: Location(world, 0.0, 0.0, 0.0)
        
        createScaffolding()
    }
    
    private fun handleUpgradeUpdates() {
        updateEnergyPerTick()
        
        maxSize = MAX_SIZE + upgradeHolder.getRangeModifier()
        drillSpeedMultiplier = DRILL_SPEED_MULTIPLIER * upgradeHolder.getSpeedModifier()
        moveSpeed = MOVE_SPEED * upgradeHolder.getSpeedModifier()
    }
    
    private fun updateBounds(): Boolean {
        val positions = getMinMaxPositions(location, sizeX, sizeZ, getFace(BlockSide.BACK), getFace(BlockSide.RIGHT))
        minX = positions[0]
        minZ = positions[1]
        maxX = positions[2]
        maxZ = positions[3]
        
        updateEnergyPerTick()
        
        if (!canPlace(ownerUUID, location, positions)) {
            if (sizeX == MIN_SIZE && sizeZ == MIN_SIZE) {
                runTaskLater(3) { TileEntityManager.destroyAndDropTileEntity(this, true) }
                return false
            } else resize(MIN_SIZE, MIN_SIZE)
        }
        
        return true
    }
    
    private fun resize(sizeX: Int, sizeZ: Int) {
        this.sizeX = sizeX
        this.sizeZ = sizeZ
        
        if (updateBounds()) {
            drilling = false
            drillProgress = 0.0
            done = false
            pointerDestination = null
            pointerLocation = Location(world, minX + 1.5, y - 2.0, minZ + 1.5)
            
            solidScaffolding.removeAllModels()
            armX.removeAllModels()
            armY.removeAllModels()
            armZ.removeAllModels()
            drill.removeAllModels()
            
            createScaffolding()
        }
    }
    
    private fun updateEnergyPerTick() {
        energyPerTick = ((BASE_ENERGY_CONSUMPTION + sizeX * sizeZ * ENERGY_PER_SQUARE_BLOCK)
            * upgradeHolder.getSpeedModifier() / upgradeHolder.getEfficiencyModifier()).toInt()
    }
    
    override fun saveData() {
        super.saveData()
        storeData("sizeX", sizeX)
        storeData("sizeZ", sizeZ)
        storeData("pointerLocation", pointerLocation)
        storeData("lastPointerLocation", lastPointerLocation)
        storeData("pointerDestination", pointerDestination)
        storeData("drillProgress", drillProgress)
        storeData("drilling", drilling)
        storeData("done", done)
    }
    
    override fun handleTick() {
        if (energyHolder.energy < energyPerTick) return
        
        if (!done) {
            if (!drilling) {
                val pointerDestination = pointerDestination ?: selectNextDestination()
                if (pointerDestination != null) {
                    if (pointerLocation.distance(pointerDestination) > 0.2) {
                        moveToPointer(pointerDestination)
                    } else {
                        pointerLocation = pointerDestination.clone()
                        pointerDestination.y -= 1
                        drilling = true
                    }
                } else done = true
            } else drill()
            
            energyHolder.energy -= energyPerTick
        }
        
    }
    
    override fun handleAsyncTick() {
        if (!done && energyHolder.energy > energyPerTick)
            updatePointer()
    }
    
    private fun moveToPointer(pointerDestination: Location) {
        val deltaX = pointerDestination.x - pointerLocation.x
        val deltaY = pointerDestination.y - pointerLocation.y
        val deltaZ = pointerDestination.z - pointerLocation.z
        
        var moveX = 0.0
        var moveY = 0.0
        var moveZ = 0.0
        
        val moveSpeed = currentMoveSpeed
        
        if (deltaY > 0) {
            moveY = deltaY.coerceIn(-moveSpeed, moveSpeed)
        } else {
            var distance = 0.0
            moveX = deltaX.coerceIn(-moveSpeed, moveSpeed)
            distance += moveX
            moveZ = deltaZ.coerceIn(-(moveSpeed - distance), moveSpeed - distance)
            distance += moveZ
            if (distance == 0.0) moveY = deltaY.coerceIn(-moveSpeed, moveSpeed)
        }
        
        pointerLocation.add(moveX, moveY, moveZ)
    }
    
    private fun drill() {
        val block = pointerDestination!!.block
        spawnDrillParticles(block)
        
        val drillSpeed = min(DRILL_SPEED_CLAMP, block.type.breakSpeed * currentDrillSpeedMultiplier)
        drillProgress += drillSpeed
        pointerLocation.y -= drillSpeed - max(0.0, drillProgress - 1)
        
        block.setBreakState(entityId, (drillProgress * 9).roundToInt())
        
        if (drillProgress >= 1f) { // is done drilling
            val drops = block.breakAndTakeDrops()
            drops.forEach { drop ->
                val leftover = inventory.addItem(null, drop)
                if (leftover != 0) {
                    drop.amount = leftover
                    world.dropItemNaturally(block.location, drop)
                }
            }
            
            pointerDestination = null
            drillProgress = 0.0
            drilling = false
        }
    }
    
    private fun updatePointer(force: Boolean = false) {
        val pointerLocation = pointerLocation.clone()
        
        if (force || lastPointerLocation.z != pointerLocation.z)
            armX.useArmorStands { it.teleport { z = pointerLocation.z } }
        if (force || lastPointerLocation.x != pointerLocation.x)
            armZ.useArmorStands { it.teleport { x = pointerLocation.x } }
        if (force || lastPointerLocation.x != pointerLocation.x || lastPointerLocation.z != pointerLocation.z)
            armY.useArmorStands { it.teleport { x = pointerLocation.x; z = pointerLocation.z } }
        
        if (force || lastPointerLocation.y != pointerLocation.y) {
            for (y in y - 1 downTo pointerLocation.blockY + 1) {
                val location = pointerLocation.clone()
                location.y = y.toDouble()
                if (!armY.hasModelLocation(location)) armY.addModels(Model(FULL_SLIM_VERTICAL, location))
            }
            armY.removeIf { armorStand, _ -> armorStand.location.blockY - 1 < pointerLocation.blockY }
        }
        
        drill.useArmorStands {
            val location = pointerLocation.clone()
            location.yaw = it.location.yaw.mod(360f)
            if (drilling) location.yaw += 25f * (2 - drillProgress.toFloat())
            else location.yaw += 10f
            it.teleport(location)
        }
        
        lastPointerLocation = pointerLocation
    }
    
    // TODO: optimize
    private fun selectNextDestination(): Location? {
        val destination = LocationUtils.getTopBlocksBetween(
            world,
            minX + 1, 0, minZ + 1,
            maxX - 1, y - 2, maxZ - 1
        ).asSequence()
            .sortedBy { prioritizedDistance(pointerLocation, it) }
            .firstOrNull { ProtectionManager.canBreak(ownerUUID, it) && (it.block.type.isBreakable() || TileEntityManager.getTileEntityAt(it) != null) }
            ?.center()
            ?.apply { y += 1 }
        
        pointerDestination = destination
        return destination
    }
    
    /**
     * Returns the square of a modified distance that discourages travelling downwards
     * and encourages travelling upwards.
     */
    private fun prioritizedDistance(location: Location, destination: Location): Double {
        val deltaX = destination.x - location.x
        val deltaZ = destination.z - location.z
        
        // encourage travelling up, discourage travelling down
        var deltaY = (destination.y - location.y)
        if (deltaY > 0) deltaY *= 0.05
        else if (deltaY < 0) deltaY *= 2
        
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
    }
    
    private fun spawnDrillParticles(block: Block) {
        // block cracks
        particleBuilder(ParticleEffect.BLOCK_CRACK, block.location.center().apply { y += 1 }) {
            texture(block.type)
            offsetX(0.2f)
            offsetZ(0.2f)
            speed(0.5f)
        }.display()
        
        // smoke
        particleBuilder(ParticleEffect.SMOKE_NORMAL, pointerLocation.clone().apply { y -= 0.1 }) {
            amount(10)
            speed(0.02f)
        }.display()
    }
    
    private fun createScaffolding() {
        runAsyncTask {
            createScaffoldingOutlines()
            createScaffoldingCorners()
            createScaffoldingPillars()
            createScaffoldingArms()
            drill.addModels(Model(DRILL, pointerLocation))
            updatePointer(true)
        }
    }
    
    private fun createScaffoldingOutlines() {
        val min = Location(location.world, minX.toDouble(), location.y, minZ.toDouble())
        val max = Location(location.world, maxX.toDouble(), location.y, maxZ.toDouble())
        
        min.getRectangle(max, true).forEach { (axis, locations) ->
            locations.forEach { createHorizontalScaffolding(solidScaffolding, it, axis) }
        }
    }
    
    private fun createScaffoldingArms() {
        val baseLocation = pointerLocation.clone().also { it.y = y.toDouble() }
        
        val armXLocations = LocationUtils.getStraightLine(baseLocation, Axis.X, minX..maxX)
        armXLocations.withIndex().forEach { (index, location) ->
            location.x += 0.5
            if (index == 0 || index == armXLocations.size - 1) {
                createSmallHorizontalScaffolding(
                    armX,
                    location.apply { yaw = if (index == 0) 180f else 0f },
                    Axis.X,
                    center = false
                )
            } else {
                createHorizontalScaffolding(armX, location, Axis.X, false)
            }
        }
        
        val armZLocations = LocationUtils.getStraightLine(baseLocation, Axis.Z, minZ..maxZ)
        armZLocations.withIndex().forEach { (index, location) ->
            location.z += 0.5
            if (index == 0 || index == armZLocations.size - 1) {
                createSmallHorizontalScaffolding(armZ,
                    location.apply { yaw = if (index == 0) 0f else 180f },
                    Axis.Z,
                    center = false
                )
            } else {
                createHorizontalScaffolding(armZ, location, Axis.Z, false)
            }
        }
        
        armY.addModels(Model(SLIM_VERTICAL_DOWN, baseLocation.clone()))
    }
    
    private fun createScaffoldingPillars() {
        for (corner in getCornerLocations(location.y)) {
            corner.y -= 1
            
            val blockBelow = corner.getNextBlockBelow(countSelf = true, requiresSolid = true)
            if (blockBelow != null && blockBelow.positionEquals(corner)) continue
            
            corner
                .getStraightLine(Axis.Y, blockBelow?.blockY?.plus(1) ?: 0)
                .forEach { createVerticalScaffolding(solidScaffolding, it) }
        }
    }
    
    
    private fun createScaffoldingCorners() {
        val y = location.y
        
        val corners = getCornerLocations(y)
            .filterNot { it.blockLocation == location }
            .map { it.center() }
        
        solidScaffolding.addModels(corners.map { Model(CORNER_DOWN, it) })
    }
    
    private fun getCornerLocations(y: Double) =
        listOf(
            Location(world, minX.toDouble(), y, minZ.toDouble()),
            Location(world, maxX.toDouble(), y, minZ.toDouble(), 90f, 0f),
            Location(world, maxX.toDouble(), y, maxZ.toDouble(), 180f, 0f),
            Location(world, minX.toDouble(), y, maxZ.toDouble(), 270f, 0f)
        )
    
    private fun createSmallHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis, center: Boolean = true) {
        location.yaw += if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(SMALL_HORIZONTAL, if (center) location.center() else location))
    }
    
    private fun createHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis, center: Boolean = true) {
        location.yaw = if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(FULL_HORIZONTAL, if (center) location.center() else location))
    }
    
    private fun createVerticalScaffolding(model: MultiModel, location: Location) {
        model.addModels(Model(FULL_VERTICAL, location.center()))
    }
    
    companion object {
        
        fun canPlace(player: Player, location: Location): Boolean {
            return canPlace(player.uniqueId, location, location.yaw, MIN_SIZE, MIN_SIZE)
        }
        
        private fun canPlace(uuid: UUID, location: Location, yaw: Float, sizeX: Int, sizeZ: Int): Boolean {
            val positions = getMinMaxPositions(
                location,
                sizeX, sizeZ,
                BlockSide.BACK.getBlockFace(yaw),
                BlockSide.RIGHT.getBlockFace(yaw)
            )
            
            return canPlace(uuid, location, positions)
        }
        
        private fun canPlace(uuid: UUID, location: Location, positions: IntArray): Boolean {
            val minLoc = Location(location.world, positions[0].toDouble(), location.y, positions[1].toDouble())
            val maxLoc = Location(location.world, positions[2].toDouble(), location.y, positions[3].toDouble())
            
            minLoc.fullCuboidTo(maxLoc) {
                if (ProtectionManager.canBreak(uuid, it))
                    return@fullCuboidTo true
                else return@canPlace false
            }
            
            return true
        }
        
        private fun getMinMaxPositions(location: Location, sizeX: Int, sizeZ: Int, back: BlockFace, right: BlockFace): IntArray {
            val modX = back.modX.takeUnless { it == 0 } ?: right.modX
            val modZ = back.modZ.takeUnless { it == 0 } ?: right.modZ
            
            val distanceX = modX * (sizeX + 1)
            val distanceZ = modZ * (sizeZ + 1)
            
            val minX = min(location.blockX, location.blockX + distanceX)
            val minZ = min(location.blockZ, location.blockZ + distanceZ)
            val maxX = max(location.blockX, location.blockX + distanceX)
            val maxZ = max(location.blockZ, location.blockZ + distanceZ)
            
            return intArrayOf(minX, minZ, maxX, maxZ)
        }
        
    }
    
    inner class QuarryGUI : TileEntityGUI("menu.nova.quarry") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Quarry,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default")
        ) { openWindow(it) }
        
        private val sizeItems = ArrayList<Item>()
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # # # . |" +
                "| # # # i i i . |" +
                "| m n p i i i . |" +
                "| # # # i i i . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('n', NumberDisplayItem { sizeX }.also(sizeItems::add))
            .addIngredient('p', AddNumberItem({ MIN_SIZE..maxSize }, { sizeX }, ::setSize).also(sizeItems::add))
            .addIngredient('m', RemoveNumberItem({ MIN_SIZE..maxSize }, { sizeX }, ::setSize).also(sizeItems::add))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4, energyHolder)
        
        private fun setSize(size: Int) {
            resize(size, size)
            sizeItems.forEach(Item::notifyWindows)
        }
        
        private inner class NumberDisplayItem(private val getNumber: () -> Int) : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                val number = getNumber()
                return NovaMaterialRegistry.NUMBER.item.createItemBuilder(getNumber())
                    .setDisplayName(TranslatableComponent("menu.nova.quarry.size", number, number))
                    .addLoreLines(localized(ChatColor.GRAY, "menu.nova.quarry.size_tip"))
            }
            
            override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
            
        }
        
    }
    
}