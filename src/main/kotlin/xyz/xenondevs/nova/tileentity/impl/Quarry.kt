package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.Model
import xyz.xenondevs.nova.tileentity.MultiModel
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import xyz.xenondevs.particle.data.texture.BlockTexture
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private val SCAFFOLDING_STACKS = NovaMaterial.SCAFFOLDING.block!!.let { modelData -> modelData.dataArray.indices.map { modelData.getItem(it) } }
private val FULL_HORIZONTAL = SCAFFOLDING_STACKS[0]
private val FULL_VERTICAL = SCAFFOLDING_STACKS[1]
private val CORNER_DOWN = SCAFFOLDING_STACKS[2]
private val SMALL_HORIZONTAL = SCAFFOLDING_STACKS[3]
private val FULL_SLIM_VERTICAL = SCAFFOLDING_STACKS[4]
private val SLIM_VERTICAL_DOWN = SCAFFOLDING_STACKS[5]
private val DRILL = SCAFFOLDING_STACKS[6]

private const val SIZE_X = 10
private const val SIZE_Z = 10

private const val MOVE_SPEED = 0.15
private const val DRILL_SPEED_MULTIPLIER = 0.25
private const val DRILL_SPEED_CLAMP = 0.5

private const val MAX_ENERGY = 100_000
private const val ENERGY_PER_TICK = 1000

class Quarry(
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val gui by lazy { QuarryGUI() }
    private val inventory = getInventory("quarryInventory", 18, true) {}
    
    private val entityId = uuid.hashCode()
    
    private val solidScaffolding = getMultiModel("solidScaffolding")
    private val armX = getMultiModel("armX")
    private val armZ = getMultiModel("armZ")
    private val armY = getMultiModel("armY")
    private val drill = getMultiModel("drill")
    
    private val y: Int
    private val minX: Int
    private val minZ: Int
    private val maxX: Int
    private val maxZ: Int
    
    private var lastPointerLocation: Location
    private var pointerLocation: Location
    private var pointerDestination: Location? = null
    
    private var drillProgress = 0.0
    private var drilling = false
    private var done = false
    
    init {
        setDefaultInventory(inventory)
        
        val back = getFace(BlockSide.BACK)
        val right = getFace(BlockSide.RIGHT)
        val modX = back.modX.takeUnless { it == 0 } ?: right.modX
        val modZ = back.modZ.takeUnless { it == 0 } ?: right.modZ
        
        val sizeX = modX * SIZE_X
        val sizeZ = modZ * SIZE_Z
        
        y = location.blockY
        minX = min(location.blockX, location.blockX + sizeX)
        minZ = min(location.blockZ, location.blockZ + sizeZ)
        maxX = max(location.blockX, location.blockX + sizeX)
        maxZ = max(location.blockZ, location.blockZ + sizeZ)
        
        pointerLocation = Location(world, minX + 1.5, y - 2.0, minZ + 1.5)
        lastPointerLocation = Location(world, 0.0, 0.0, 0.0)
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        if (first) {
            createScaffolding()
            drill.addModels(Model(DRILL, pointerLocation))
            runTaskLater(1) { updatePointer() }
        }
    }
    
    override fun handleTick() {
        if (hasEnergyChanged) gui.energyBar.update()
        if (energy >= ENERGY_PER_TICK) {
            energy -= ENERGY_PER_TICK
        } else return
        
        if (!done && !drilling) {
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
        } else if (drilling) drill()
    }
    
    private fun moveToPointer(pointerDestination: Location) {
        val deltaX = pointerDestination.x - pointerLocation.x
        val deltaY = pointerDestination.y - pointerLocation.y
        val deltaZ = pointerDestination.z - pointerLocation.z
        
        var moveX = 0.0
        var moveY = 0.0
        var moveZ = 0.0
        
        if (deltaY > 0) {
            moveY = deltaY.coerceIn(-MOVE_SPEED, MOVE_SPEED)
        } else {
            var distance = 0.0
            moveX = deltaX.coerceIn(-MOVE_SPEED, MOVE_SPEED)
            distance += moveX
            moveZ = deltaZ.coerceIn(-(MOVE_SPEED - distance), MOVE_SPEED - distance)
            distance += moveZ
            if (distance == 0.0) moveY = deltaY.coerceIn(-MOVE_SPEED, MOVE_SPEED)
        }
        
        pointerLocation.add(moveX, moveY, moveZ)
        
        updatePointer()
    }
    
    private fun drill() {
        val block = pointerDestination!!.block
        spawnDrillParticles(block)
        
        val drillSpeed = min(min(DRILL_SPEED_CLAMP, 1.0), block.type.breakSpeed * DRILL_SPEED_MULTIPLIER)
        drillProgress += drillSpeed
        pointerLocation.y -= drillSpeed - max(0.0, drillProgress - 1)
        
        updateBreakState(block)
        
        if (drillProgress >= 1f) { // is done drilling
            spawnBreakParticles(block)
            playBreakSound(block)
            
            val tileEntity = TileEntityManager.getTileEntityAt(block.location)
            if (tileEntity != null) {
                val drops = TileEntityManager.destroyTileEntity(tileEntity, true)
                if (inventory.canFit(drops)) inventory.addAll(null, drops)
                else block.world.dropItemsNaturally(block.location, drops)
            } else {
                val drops = block.drops.toList()
                if (inventory.canFit(drops)) {
                    inventory.addAll(null, drops)
                    block.type = Material.AIR
                } else block.breakNaturally()
            }
            
            pointerDestination = null
            drillProgress = 0.0
            drilling = false
        }
        
        updatePointer()
    }
    
    private fun updatePointer() {
        if (lastPointerLocation.z != pointerLocation.z)
            armX.useArmorStands { it.teleport { z = pointerLocation.z } }
        if (lastPointerLocation.x != pointerLocation.x)
            armZ.useArmorStands { it.teleport { x = pointerLocation.x } }
        if (lastPointerLocation.x != pointerLocation.x || lastPointerLocation.z != pointerLocation.z)
            armY.useArmorStands { it.teleport { x = pointerLocation.x; z = pointerLocation.z } }
        if (lastPointerLocation.y != pointerLocation.y) updateVerticalArmModels()
        
        drill.useArmorStands {
            val location = pointerLocation.clone()
            location.yaw = it.location.yaw.mod(360f)
            if (drilling) location.yaw += 25f * (2 - drillProgress.toFloat())
            else location.yaw += 10f
            it.teleport(location)
        }
        
        lastPointerLocation = pointerLocation.clone()
    }
    
    private fun updateVerticalArmModels() {
        for (y in y - 1 downTo pointerLocation.blockY + 1) {
            val location = pointerLocation.clone()
            location.y = y.toDouble()
            if (!armY.hasModelLocation(location)) armY.addModels(Model(FULL_SLIM_VERTICAL, location))
        }
        armY.removeIf { armorStand, _ -> armorStand.location.y - 1 < pointerLocation.y }
    }
    
    private fun selectNextDestination(): Location? {
        val destination = LocationUtils.getTopBlocksBetween(
            world,
            minX + 1, 0, minZ + 1,
            maxX - 1, y - 2, maxZ - 1
        )
            .filter { it.block.type.isBreakable() || TileEntityManager.getTileEntityAt(it) != null }
            .sortedBy { it.distance(pointerLocation) }
            .maxByOrNull { it.y }
            ?.center()
            ?.apply { y += 1 }
        
        pointerDestination = destination
        return destination
    }
    
    private fun updateBreakState(block: Block) {
        val breakPacket = ReflectionUtils.createBlockBreakAnimationPacket(
            entityId,
            ReflectionUtils.createBlockPosition(block.location),
            (drillProgress * 9).roundToInt()
        )
        
        block.chunk.getSurroundingChunks(1, true)
            .flatMap { it.entities.toList() }
            .filterIsInstance<Player>()
            .forEach { ReflectionUtils.sendPacket(it, breakPacket) }
    }
    
    private fun spawnBreakParticles(block: Block) {
        ParticleBuilder(ParticleEffect.BLOCK_CRACK, block.location.add(0.5, 0.5, 0.5))
            .setParticleData(BlockTexture(block.type))
            .setOffsetX(0.2f)
            .setOffsetY(0.2f)
            .setOffsetZ(0.2f)
            .setAmount(50)
            .display()
    }
    
    private fun playBreakSound(block: Block) {
        val breakSound = SoundUtils.getSoundEffects(block)[0]
        world.playSound(block.location, breakSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
    }
    
    private fun spawnDrillParticles(block: Block) {
        // block cracks
        ParticleBuilder(ParticleEffect.BLOCK_CRACK, block.location.center().apply { y += 1 })
            .setParticleData(BlockTexture(block.type))
            .setOffsetX(0.2f)
            .setOffsetZ(0.2f)
            .setSpeed(0.5f)
            .display()
        
        // smoke
        ParticleBuilder(ParticleEffect.SMOKE_NORMAL,
            pointerLocation.clone().apply { y -= 0.1 }
        ).setAmount(10).setSpeed(0.02f).display()
    }
    
    private fun createScaffolding() {
        createScaffoldingLines()
        createScaffoldingCorners()
        createScaffoldingPillars()
    }
    
    private fun createScaffoldingLines() {
        val min = Location(location.world, minX.toDouble(), location.y, minZ.toDouble())
        val max = Location(location.world, maxX.toDouble(), location.y, maxZ.toDouble())
        
        min.getRectangle(max, true).forEach { (axis, locations) ->
            locations.forEach { createHorizontalScaffolding(solidScaffolding, it, axis) }
        }
        
        val armXLocations = min.getStraightLine(Axis.X, max.blockX)
        armXLocations.withIndex().forEach { (index, location) ->
            if (index == 0 || index == armXLocations.size - 1) {
                createSmallHorizontalScaffolding(armX, location.apply { yaw = if (index == 0) 180f else 0f }, Axis.X)
            } else {
                createHorizontalScaffolding(armX, location, Axis.X)
            }
        }
        
        val armZLocations = min.getStraightLine(Axis.Z, max.blockZ)
        armZLocations.withIndex().forEach { (index, location) ->
            if (index == 0 || index == armZLocations.size - 1) {
                createSmallHorizontalScaffolding(armZ, location.apply { yaw = if (index == 0) 0f else 180f }, Axis.Z)
            } else {
                createHorizontalScaffolding(armZ, location, Axis.Z)
            }
        }
        
        armY.addModels(Model(SLIM_VERTICAL_DOWN, location))
    }
    
    private fun createScaffoldingPillars() {
        getCornerLocations(location.y).forEach { corner ->
            corner
                .subtract(0.0, 1.0, 0.0)
                .getStraightLine(Axis.Y, 0)
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
    
    private fun createSmallHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis) {
        location.yaw += if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(SMALL_HORIZONTAL, location.center()))
    }
    
    private fun createHorizontalScaffolding(model: MultiModel, location: Location, axis: Axis) {
        location.yaw = if (axis == Axis.Z) 0f else 90f
        model.addModels(Model(FULL_HORIZONTAL, location.center()))
    }
    
    private fun createVerticalScaffolding(model: MultiModel, location: Location) {
        model.addModels(Model(FULL_VERTICAL, location.center()))
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    private inner class QuarryGUI {
        
        private val sideConfigGUI = SideConfigGUI(
            this@Quarry,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(inventory to "Quarry Inventory")
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # . |" +
                "| . . . . . . . |" +
                "| . . . . . . . |" +
                "| . . . . . . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
            .also { it.fillRectangle(1, 2, 6, inventory, true) }
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 4) { energy to MAX_ENERGY }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Quarry", gui).show()
        }
        
    }
    
}