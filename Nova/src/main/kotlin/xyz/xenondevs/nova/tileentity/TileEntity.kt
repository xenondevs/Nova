package xyz.xenondevs.nova.tileentity

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.InventoryUpdatedEvent
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.virtualinventory.event.UpdateReason
import de.studiocode.invui.window.impl.single.SimpleWindow
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.container.NovaFluidContainer
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.overlay.GUITexture
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import xyz.xenondevs.nova.world.block.TileEntityBlock
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.region.Region
import java.util.*
import xyz.xenondevs.nova.api.tileentity.TileEntity as ITileEntity

abstract class TileEntity(val blockState: NovaTileEntityState) : DataHolder(true), Reloadable, ITileEntity {
    
    companion object {
        val SELF_UPDATE_REASON = object : UpdateReason {}
        val TILE_ENTITY_KEY = NamespacedKey(NOVA, "tileEntityData")
    }
    
    override var legacyData: LegacyCompound? = blockState.legacyData
    
    val pos: BlockPos = blockState.pos
    val uuid: UUID = blockState.uuid
    val ownerUUID: UUID = blockState.ownerUUID
    final override val data: Compound = blockState.data
    final override val material: TileEntityNovaMaterial = blockState.material
    
    override val owner: OfflinePlayer by lazy { Bukkit.getOfflinePlayer(ownerUUID) }
    
    val location: Location
        get() = Location(pos.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), facing.getYaw(BlockFace.NORTH), 0f)
    val centerLocation: Location
        get() = location.center()
    val world: World
        get() = pos.world
    val chunk: Chunk
        get() = chunkPos.chunk!!
    val chunkPos: ChunkPos
        get() = pos.chunkPos
    val facing: BlockFace
        get() = blockState.getProperty(Directional::class)?.facing ?: BlockFace.NORTH
    
    @Volatile
    var isValid: Boolean = true
        private set
    
    abstract val gui: Lazy<TileEntityGUI>?
    
    private val multiModels = ArrayList<MultiModel>()
    private val particleTasks = ArrayList<TileEntityParticleTask>()
    
    private val _inventories = HashMap<VirtualInventory, Boolean>()
    private val _fluidContainers = HashMap<NovaFluidContainer, Boolean>()
    
    val inventories: List<VirtualInventory>
        get() = _inventories.keys.toList()
    val fluidContainers: List<FluidContainer>
        get() = _fluidContainers.keys.toList()
    
    override fun getDrops(includeSelf: Boolean): MutableList<ItemStack> {
        val drops = ArrayList<ItemStack>()
        if (includeSelf) {
            saveData()
            
            val item = material.createItemStack()
            if (globalData.isNotEmpty()) {
                val itemMeta = item.itemMeta!!
                itemMeta.persistentDataContainer.set(TILE_ENTITY_KEY, globalData)
                item.itemMeta = itemMeta
            }
            
            drops += item
        }
        
        if (this is Upgradable) drops += this.upgradeHolder.dropUpgrades()
        _inventories.forEach { (inv, global) -> if (!global) drops += inv.items.filterNotNull() }
        return drops
    }
    
    /**
     * Called to save all data using the [storeData] method.
     */
    open fun saveData() {
        if (this is Upgradable)
            upgradeHolder.save(data)
        
        _inventories.forEach { (inv, global) ->
            storeData("inventory.${inv.uuid}", inv, global)
        }
        
        _fluidContainers.forEach { (con, global) ->
            val compound = Compound()
            compound["amount"] = con.amount
            compound["type"] = con.type
            
            storeData("fluidContainer.${con.uuid}", compound, global)
        }
    }
    
    /**
     * Called every tick for every TileEntity that is in a loaded chunk.
     */
    open fun handleTick() = Unit
    
    /**
     * Called asynchronously for every tick that this TileEntity is in a loaded chunk.
     */
    open fun handleAsyncTick() = Unit
    
    /**
     * Called after the TileEntity has been initialized and added to the
     * TileEntity map in the TileEntityManager.
     *
     * The [first] parameter specifies if it is the first time this
     * [TileEntity] got initialized, meaning it has just been placed.
     */
    open fun handleInitialized(first: Boolean) = Unit
    
    /**
     * Called after the TileEntity has been removed from the
     * TileEntityManager's TileEntity map because it either got
     * unloaded or destroyed.
     *
     * @param unload If the [TileEntity] was removed because the chunk got unloaded.
     */
    open fun handleRemoved(unload: Boolean) {
        isValid = false
        if (gui?.isInitialized() == true) gui!!.value.closeWindows()
        
        multiModels.forEach { it.close() }
        particleTasks.forEach { it.stop() }
    }
    
    /**
     * Called when a [TileEntityBlock] is interacted with.
     *
     * Might be called twice for each hand.
     *
     * @return If any action was performed.
     */
    open fun handleRightClick(ctx: BlockInteractContext): Boolean {
        val player = ctx.source as? Player ?: return false
        if (gui != null && !player.hasInventoryOpen) {
            gui!!.value.openWindow(player)
            return true
        }
        return false
    }
    
    fun getUpgradeHolder(vararg allowed: UpgradeType<*>): UpgradeHolder =
        UpgradeHolder(this, gui!!, ::reload, *allowed)
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        stackSizes: IntArray,
        global: Boolean = false,
        preUpdateHandler: ((ItemUpdateEvent) -> Unit)? = null,
        afterUpdateHandler: ((InventoryUpdatedEvent) -> Unit)? = null,
    ): VirtualInventory {
        val invUUID = uuid.salt(salt)
        val inventory = retrieveData("inventory.$invUUID") {
            VirtualInventory(invUUID, size, arrayOfNulls(size), stackSizes)
        }
        
        if (preUpdateHandler != null) inventory.setItemUpdateHandler(preUpdateHandler)
        if (afterUpdateHandler != null) inventory.setInventoryUpdatedHandler(afterUpdateHandler)
        
        _inventories[inventory] = global
        return inventory
    }
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        preUpdateHandler: ((ItemUpdateEvent) -> Unit)? = null,
        afterUpdateHandler: ((InventoryUpdatedEvent) -> Unit)? = null,
    ) = getInventory(salt, size, IntArray(size) { 64 }, false, preUpdateHandler, afterUpdateHandler)
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        global: Boolean = false,
        preUpdateHandler: ((ItemUpdateEvent) -> Unit)? = null,
        afterUpdateHandler: ((InventoryUpdatedEvent) -> Unit)? = null,
    ) = getInventory(salt, size, IntArray(size) { 64 }, global, preUpdateHandler, afterUpdateHandler)
    
    /**
     * Gets a [FluidContainer] for this [TileEntity].
     */
    fun getFluidContainer(
        name: String,
        types: Set<FluidType>,
        capacity: ValueReloadable<Long>,
        defaultAmount: Long = 0,
        updateHandler: (() -> Unit)? = null,
        upgradeHolder: UpgradeHolder? = null,
        global: Boolean = true
    ): FluidContainer {
        val uuid = UUID.nameUUIDFromBytes(name.toByteArray())
        
        val storedAmount: Long?
        val storedType: FluidType?
        
        if (legacyData != null) {
            val fluidData = retrieveOrNull<LegacyCompound>("fluidContainer.$uuid")
            storedAmount = fluidData?.get<Long>("amount")
            storedType = fluidData?.get<FluidType>("type")
        } else {
            val fluidData = retrieveOrNull<Compound>("fluidContainer.$uuid")
            storedAmount = fluidData?.get<Long>("amount")
            storedType = fluidData?.get<FluidType>("type")
        }
        
        val container = NovaFluidContainer(uuid, types, storedType ?: FluidType.NONE, storedAmount
            ?: defaultAmount, capacity, upgradeHolder)
        
        updateHandler?.apply(container.updateHandlers::add)
        _fluidContainers[container] = global
        return container
    }
    
    /**
     * Creates a new [MultiModel] for this [TileEntity].
     * When the [TileEntity] is removed, all [Models][Model] belonging
     * to this [MultiModel] will be removed.
     */
    fun createMultiModel(): MultiModel {
        return MultiModel().also(multiModels::add)
    }
    
    /**
     * Creates a new [TileEntityParticleTask] for this [TileEntity].
     * When the [TileEntity] is removed, the [TileEntityParticleTask]
     * will automatically be stopped as well.
     */
    fun createParticleTask(particles: List<Any>, tickDelay: Int): TileEntityParticleTask {
        val task = TileEntityParticleTask(this, particles, tickDelay)
        particleTasks += task
        return task
    }
    
    /**
     * Plays a sound effect to all viewers of this [TileEntity]
     */
    fun playSoundEffect(sound: Sound, volume: Float, pitch: Float) {
        getViewers().forEach {
            it.playSound(location, sound, volume, pitch)
        }
    }
    
    /**
     * Plays a sound effect to all viewers of this [TileEntity]
     */
    fun playSoundEffect(sound: String, volume: Float, pitch: Float) {
        getViewers().forEach {
            it.playSound(location, sound, volume, pitch)
        }
    }
    
    /**
     * Gets a [List] of all [players][Player] that this [TileEntity] is
     * visible for.
     */
    fun getViewers(): List<Player> =
        FakeArmorStandManager.getChunkViewers(chunkPos)
    
    /**
     * Gets the correct direction a block side.
     */
    fun getFace(blockSide: BlockSide): BlockFace =
        blockSide.getBlockFace(facing.yaw)
    
    /**
     * Creates a side config
     */
    fun createSideConfig(
        default: NetworkConnectionType,
        vararg blocked: BlockSide
    ): EnumMap<BlockFace, NetworkConnectionType> {
        
        val sideConfig = EnumMap<BlockFace, NetworkConnectionType>(BlockFace::class.java)
        val blockedFaces = blocked.map { getFace(it) }
        CUBE_FACES.forEach { sideConfig[it] = if (blockedFaces.contains(it)) NetworkConnectionType.NONE else default }
        
        return sideConfig
    }
    
    /**
     * Creates a side config
     */
    fun createExclusiveSideConfig(
        type: NetworkConnectionType,
        vararg sides: BlockSide
    ): EnumMap<BlockFace, NetworkConnectionType> {
        
        val sideFaces = sides.map(::getFace)
        return CUBE_FACES.associateWithTo(emptyEnumMap()) {
            if (it in sideFaces) type else NetworkConnectionType.NONE
        }
    }
    
    /**
     * Creates a [Region] of a specified [size] that surrounds this [TileEntity].
     */
    fun getSurroundingRegion(size: Int): Region {
        val d = size + 0.5
        return Region(
            location.clone().center().subtract(d, d, d),
            location.clone().center().add(d, d, d)
        )
    }
    
    /**
     * Creates a block [Region] with the given [length], [width], [height] and
     * [vertical translation][translateVertical] in front of this [TileEntity].
     */
    fun getBlockFrontRegion(length: Int, width: Int, height: Int, translateVertical: Int): Region {
        return getFrontRegion(length * 2.0 + 1, width + 0.5, width + 0.5, height.toDouble(), translateVertical.toDouble())
    }
    
    /**
     * Creates a [Region] with the  given [length], [width], [height] and
     * [vertical translation][translateVertical] in front of this [TileEntity].
     */
    fun getFrontRegion(length: Double, width: Double, height: Double, translateVertical: Double): Region {
        return getFrontRegion(length, width / 2.0, width / 2.0, height, translateVertical)
    }
    
    /**
     * Creates a [Region] with the  given [length], [left] and [right] movement, [height] and
     * [vertical translation][translateVertical] in front of this [TileEntity].
     */
    fun getFrontRegion(length: Double, left: Double, right: Double, height: Double, translateVertical: Double): Region {
        val frontFace = getFace(BlockSide.FRONT)
        val startLocation = location.clone().center().advance(frontFace, 0.5)
        
        val pos1 = startLocation.clone().apply {
            advance(getFace(BlockSide.LEFT), left)
            y += translateVertical
        }
        
        val pos2 = startLocation.clone().apply {
            advance(getFace(BlockSide.RIGHT), right)
            advance(frontFace, length)
            y += height + translateVertical
        }
        
        return Region(LocationUtils.sort(pos1, pos2))
    }
    
    override fun equals(other: Any?): Boolean {
        return other is TileEntity && other === this
    }
    
    override fun hashCode(): Int {
        return uuid.hashCode()
    }
    
    override fun toString(): String {
        return "${javaClass.name}(Material: $material, Location: ${pos}, UUID: $uuid)"
    }
    
    abstract inner class TileEntityGUI(private val texture: GUITexture? = null) {
        
        /**
         * The main [GUI] of a [TileEntity] to be opened when it is right-clicked and closed when
         * the owning [TileEntity] is destroyed.
         */
        abstract val gui: GUI
        
        /**
         * A list of [GUIs][GUI] that are not a part of [gui] but should still be closed
         * when the [TileEntity] is destroyed.
         */
        val subGUIs = ArrayList<GUI>()
        
        /**
         * Opens a Window of the [gui] to the specified [player].
         */
        fun openWindow(player: Player) {
            val title = texture?.getTitle(material.localizedName)
                ?: arrayOf(TranslatableComponent(material.localizedName))
            SimpleWindow(player, title, gui).show()
        }
        
        /**
         * Closes all Windows connected to this [TileEntityGUI].
         */
        fun closeWindows() {
            gui.closeForAllViewers()
            subGUIs.forEach(GUI::closeForAllViewers)
        }
        
    }
    
}
