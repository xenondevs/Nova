package xyz.xenondevs.nova.tileentity

import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.menu.MenuContainer
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.container.NovaFluidContainer
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.overlay.character.gui.GuiTexture
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.LocationUtils
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.hasInventoryOpen
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.pitch
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.util.yaw
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.TileEntityBlockBehavior
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.region.DynamicRegion
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.ReloadableRegion
import xyz.xenondevs.nova.world.region.StaticRegion
import xyz.xenondevs.nova.world.region.UpgradableRegion
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass as TileEntityMenuAnnotation

abstract class TileEntity(val blockState: NovaTileEntityState) : DataHolder(true), Reloadable {
    
    companion object {
        val SELF_UPDATE_REASON = object : UpdateReason {}
        
        val TILE_ENTITY_DATA_KEY = ResourceLocation("nova", "tileentity")
    }
    
    val pos: BlockPos = blockState.pos
    val uuid: UUID = blockState.uuid
    val ownerUUID: UUID? = blockState.ownerUUID
    final override val data: Compound = blockState.data
    val block: NovaTileEntityBlock = blockState.block
    
    val owner: OfflinePlayer? by lazy { ownerUUID?.let(Bukkit::getOfflinePlayer) }
    
    val location: Location
        get() = Location(pos.world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), facing.yaw, facing.pitch)
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
    
    internal val packetTasks = ArrayList<TileEntityPacketTask>()
    private val regions = HashMap<String, ReloadableRegion>()
    
    private val _inventories = HashMap<VirtualInventory, Boolean>()
    private val _fluidContainers = HashMap<NovaFluidContainer, Boolean>()
    
    val inventories: List<VirtualInventory>
        get() = _inventories.keys.toList()
    val fluidContainers: List<FluidContainer>
        get() = _fluidContainers.keys.toList()
    
    lateinit var menuContainer: MenuContainer
    
    init {
        // look through the nested classes of this::class and all its superclasses for a class annotated with @TileEntityMenuClass
        var guiClass: KClass<*>? = null
        var clazz: KClass<*>? = this::class
        while (clazz != null && guiClass == null) {
            guiClass = clazz.nestedClasses.firstOrNull { it.hasAnnotation<TileEntityMenuAnnotation>() }
            clazz = clazz.java.superclass?.kotlin
        }
        
        // if a class was found, create a MenuContainer for it
        if (guiClass != null) {
            @Suppress("LeakingThis")
            menuContainer = MenuContainer.of(this, guiClass)
        }
    }
    
    override fun reload() {
        regions.values.forEach(ReloadableRegion::reload)
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
        
        regions.forEach { (name, region) ->
            if (region !is DynamicRegion)
                return@forEach
            
            storeData("region.$name", region.size)
        }
        
        saveDataAccessors()
    }
    
    /**
     * Gets a list of [ItemStacks][ItemStack] to be dropped when this [TileEntity] is destroyed.
     */
    open fun getDrops(includeSelf: Boolean): MutableList<ItemStack> {
        val drops = ArrayList<ItemStack>()
        if (includeSelf) {
            saveData()
            
            val item = block.item?.createItemStack()
            if (item != null) {
                if (globalData.isNotEmpty()) {
                    item.novaCompound[TILE_ENTITY_DATA_KEY] = globalData
                }
                
                drops += item
            }
        }
        
        if (this is Upgradable) drops += this.upgradeHolder.getUpgradeItems()
        _inventories.forEach { (inv, global) -> if (!global) drops += inv.items.filterNotNull() }
        return drops
    }
    
    /**
     * Gets the amount of exp to be dropped when this [TileEntity] is destroyed.
     */
    open fun getExp(): Int = 0
    
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
        packetTasks.forEach { it.stop() }
        regions.values.forEach { VisualRegion.removeRegion(it.uuid) }
        if (::menuContainer.isInitialized) menuContainer.closeWindows()
        if (this is Upgradable) upgradeHolder.handleRemoved()
    }
    
    /**
     * Called when a [TileEntityBlockBehavior] is interacted with.
     *
     * Might be called twice for each hand.
     *
     * @return If any action was performed.
     */
    open fun handleRightClick(ctx: BlockInteractContext): Boolean {
        val player = ctx.source as? Player ?: return false
        if (::menuContainer.isInitialized && !player.hasInventoryOpen) {
            menuContainer.openWindow(player)
            return true
        }
        return false
    }
    
    fun getUpgradeHolder(vararg allowed: UpgradeType<*>): UpgradeHolder {
        check(::menuContainer.isInitialized) { "A TileEntityMenu class must be present to create an UpgradeHolder" }
        return UpgradeHolder(this, menuContainer, ::reload, allowed.toHashSet())
    }
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        maxStackSizes: IntArray,
        global: Boolean = false,
        preUpdateHandler: ((ItemPreUpdateEvent) -> Unit)? = null,
        postUpdateHandler: ((ItemPostUpdateEvent) -> Unit)? = null,
    ): VirtualInventory {
        val invUUID = uuid.salt(salt)
        val inventory = retrieveData("inventory.$invUUID") { VirtualInventory(invUUID, size) }
        inventory.maxStackSizes = maxStackSizes
        if (preUpdateHandler != null) inventory.setPreUpdateHandler(preUpdateHandler)
        if (postUpdateHandler != null) inventory.setPostUpdateHandler(postUpdateHandler)
        
        _inventories[inventory] = global
        return inventory
    }
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        preUpdateHandler: ((ItemPreUpdateEvent) -> Unit)? = null,
        postUpdateHandler: ((ItemPostUpdateEvent) -> Unit)? = null,
    ): VirtualInventory = getInventory(salt, size, IntArray(size) { 64 }, false, preUpdateHandler, postUpdateHandler)
    
    /**
     * Gets a [VirtualInventory] for this [TileEntity].
     */
    fun getInventory(
        salt: String,
        size: Int,
        global: Boolean = false,
        preUpdateHandler: ((ItemPreUpdateEvent) -> Unit)? = null,
        postUpdateHandler: ((ItemPostUpdateEvent) -> Unit)? = null,
    ): VirtualInventory = getInventory(salt, size, IntArray(size) { 64 }, global, preUpdateHandler, postUpdateHandler)
    
    /**
     * Gets a [FluidContainer] for this [TileEntity].
     */
    @JvmName("getFluidContainerInternal")
    private fun getFluidContainer(
        name: String,
        types: Set<FluidType>,
        capacity: Provider<Long>,
        defaultAmount: Long = 0,
        updateHandler: (() -> Unit)? = null,
        upgradeHolder: UpgradeHolder? = null,
        upgradeType: UpgradeType<Double>? = null,
        global: Boolean = true
    ): FluidContainer {
        val uuid = UUID.nameUUIDFromBytes(name.toByteArray())
        
        val storedAmount: Long?
        val storedType: FluidType?
        
        val fluidData = retrieveDataOrNull<Compound>("fluidContainer.$uuid")
        storedAmount = fluidData?.get<Long>("amount")
        storedType = fluidData?.get<FluidType>("type")
        
        val container = NovaFluidContainer(
            uuid,
            types,
            storedType ?: FluidType.NONE,
            storedAmount ?: defaultAmount,
            capacity,
            upgradeHolder,
            upgradeType
        )
        
        if (updateHandler != null)
            container.updateHandlers += updateHandler
        
        _fluidContainers[container] = global
        return container
    }
    
    /**
     * Gets a [FluidContainer] for this [TileEntity].
     */
    fun getFluidContainer(
        name: String,
        types: Set<FluidType>,
        capacity: Provider<Long>,
        defaultAmount: Long = 0,
        updateHandler: (() -> Unit)? = null,
        upgradeHolder: UpgradeHolder,
        upgradeType: UpgradeType<Double>,
        global: Boolean = true
    ) = getFluidContainer(name, types, capacity, defaultAmount, updateHandler, upgradeHolder as UpgradeHolder?, upgradeType as UpgradeType<Double>?, global)
    
    
    /**
     * Gets a [FluidContainer] for this [TileEntity].
     */
    fun getFluidContainer(
        name: String,
        types: Set<FluidType>,
        capacity: Provider<Long>,
        defaultAmount: Long = 0,
        updateHandler: (() -> Unit)? = null,
        global: Boolean = true
    ) = getFluidContainer(name, types, capacity, defaultAmount, updateHandler, null, null, global)
    
    /**
     * Creates a new [StaticRegion] with a reloadable [size] under the name "default".
     */
    fun createStaticRegion(
        size: Provider<Int>,
        createRegion: (Int) -> Region
    ): StaticRegion = createStaticRegion("default", size, createRegion)
    
    /**
     * Creates a new [StaticRegion] with a reloadable [size] under the given [name].
     */
    fun createStaticRegion(
        name: String,
        size: Provider<Int>,
        createRegion: (Int) -> Region
    ): StaticRegion {
        check(name !in regions) { "Another region is already registered under the name $name." }
        
        val uuid = uuid.salt(name)
        val region = StaticRegion(uuid, size, createRegion)
        regions[name] = region
        
        return region
    }
    
    /**
     * Creates a new dynamic region with the reloadable bounds [minSize] and [maxSize] under the name "default".
     * If this [TileEntity] did not save a size under that name before, [defaultSize] is used.
     *
     * The configured size will be automatically saved during [TileEntity.saveData].
     */
    fun getDynamicRegion(
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): DynamicRegion = getDynamicRegion("default", minSize, maxSize, defaultSize, createRegion)
    
    /**
     * Creates a new dynamic region with the reloadable bounds [minSize] and [maxSize] under the given [name].
     * If this [TileEntity] did not save a size under that name before, [defaultSize] is used.
     *
     * The configured size will be automatically saved during [TileEntity.saveData].
     */
    fun getDynamicRegion(
        name: String,
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): DynamicRegion {
        check(name !in regions) { "Another region is already registered under the name $name." }
        
        val uuid = uuid.salt(name)
        val size = retrieveDataOrNull<Int>("region.$name") ?: defaultSize
        val region = DynamicRegion(uuid, minSize, maxSize, size, createRegion)
        regions[name] = region
        
        return region
    }
    
    /**
     * Creates a new upgradable region with the reloadable bounds [minSize] and [maxSize] under the name "default"
     * using the specified [upgradeType] as a modifier for [maxSize].
     * If this [TileEntity] did not save a size under that name before, [defaultSize] is used.
     *
     * The configured size will be automatically saved during [TileEntity.saveData].
     */
    fun getUpgradableRegion(
        upgradeType: UpgradeType<Int>,
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): UpgradableRegion = getUpgradableRegion("default", upgradeType, minSize, maxSize, defaultSize, createRegion)
    
    /**
     * Creates a new upgradable region with the reloadable bounds [minSize] and [maxSize] under the given [name]
     * using the specified [upgradeType] as a modifier for [maxSize].
     * If this [TileEntity] did not save a size under that name before, [defaultSize] is used.
     *
     * The configured size will be automatically saved during [TileEntity.saveData].
     */
    fun getUpgradableRegion(
        name: String,
        upgradeType: UpgradeType<Int>,
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): UpgradableRegion {
        check(name !in regions) { "Another region is already registered under the name $name." }
        check(this is Upgradable) { "Can't create an UpgradableRegion for a TileEntity that isn't Upgradable" }
        
        val uuid = uuid.salt(name)
        val size = retrieveDataOrNull<Int>("region.$name") ?: defaultSize
        val region = UpgradableRegion(uuid, upgradeHolder, upgradeType, minSize, maxSize, size, createRegion)
        regions[name] = region
        
        return region
    }
    
    /**
     * Creates a new [TileEntityPacketTask] for this [TileEntity].
     * All [packets] will be sent to this [TileEntity's][TileEntity] [viewers][getViewers] every [interval] ticks.
     * When the [TileEntity] is removed, the [TileEntityPacketTask] will automatically be stopped as well.
     */
    fun createPacketTask(packets: List<Packet<*>>, interval: Long): TileEntityPacketTask {
        val task = TileEntityPacketTask(this, packets, interval)
        packetTasks += task
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
        FakeEntityManager.getChunkViewers(chunkPos)
    
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
        return CUBE_FACES.associateWithTo(enumMap()) {
            if (it in sideFaces) type else NetworkConnectionType.NONE
        }
    }
    
    /**
     * Creates a [Region] of a specified [size] that surrounds this [TileEntity].
     */
    fun getSurroundingRegion(size: Int): Region {
        val d = size + 0.5
        return Region(
            location.clone().add(.5, .5, .5).subtract(d, d, d),
            location.clone().add(.5, .5, .5).add(d, d, d)
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
        return "${javaClass.name}(Material: $block, Location: ${pos}, UUID: $uuid)"
    }
    
    abstract inner class TileEntityMenu internal constructor(protected val texture: GuiTexture? = null) {
        
        open fun getTitle(): Component {
            return texture?.getTitle(block.localizedName)
                ?: Component.translatable(block.localizedName)
        }
        
    }
    
    abstract inner class GlobalTileEntityMenu(
        texture: GuiTexture? = null
    ) : TileEntityMenu(texture) {
        
        abstract val gui: Gui
        open val windowBuilder: Window.Builder<*, *> by lazy {
            Window.single()
                .setGui(gui)
                .setTitle(getTitle())
        }
        
        open fun openWindow(player: Player) {
            val window = windowBuilder.build(player)
            menuContainer.registerWindow(window)
            window.open()
        }
        
    }
    
    abstract inner class IndividualTileEntityMenu(
        protected val player: Player,
        texture: GuiTexture? = null
    ) : TileEntityMenu(texture) {
        
        abstract val gui: Gui
        open val windowBuilder: Window.Builder<*, *> by lazy {
            Window.single()
                .setGui(gui)
                .setTitle(getTitle())
        }
        
        open fun openWindow() {
            val window = windowBuilder.build(player)
            menuContainer.registerWindow(window)
            window.open()
        }
        
    }
    
}
