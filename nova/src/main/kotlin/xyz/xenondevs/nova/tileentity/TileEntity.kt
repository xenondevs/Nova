@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.tileentity

import net.kyori.adventure.text.Component
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.mapNonNull
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.invui.window.type.context.setTitle
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockInteract
import xyz.xenondevs.nova.data.context.intention.ContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.ContextParamTypes
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.tileentity.menu.MenuContainer
import xyz.xenondevs.nova.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.type.fluid.container.DynamicFluidContainer
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.hasInventoryOpen
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.behavior.TileEntityBlockBehavior
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.region.DynamicRegion
import xyz.xenondevs.nova.world.region.Region
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass as TileEntityMenuAnnotation

abstract class TileEntity(
    val pos: BlockPos,
    blockState: NovaBlockState,
    override val data: Compound
) : DataHolder(true) {
    
    companion object {
        
        /**
         * An [UpdateReason] that can be used to indicate [VirtualInventory] updates by the [TileEntity] itself.
         */
        val SELF_UPDATE_REASON = object : UpdateReason {}
        
        /**
         * The key under which [TileEntity] data is stored in [ItemStacks][ItemStack].
         */
        val TILE_ENTITY_DATA_KEY = ResourceLocation("nova", "tileentity")
        
    }
    
    /**
     * The [UUID] of this [TileEntity].
     */
    val uuid: UUID by storedValue("uuid") { UUID.randomUUID() }
    
    private var _ownerUuid = storedValue<UUID>("ownerUuid")
    
    /**
     * The [UUID] of the [Player] that placed this [TileEntity], may be null if it wasn't placed by a player.
     */
    var ownerUuid: UUID? by _ownerUuid
    
    /**
     * The [OfflinePlayer] that placed this [TileEntity], may be null if it wasn't placed by a player.
     */
    var owner: OfflinePlayer? by _ownerUuid.mapNonNull(Bukkit::getOfflinePlayer, OfflinePlayer::getUniqueId)
    
    /**
     * The current [NovaBlockState] of this [TileEntity].
     */
    var blockState: NovaBlockState = blockState
        internal set
    
    /**
     * The [NovaTileEntityBlock] this [TileEntity].
     */
    val block: NovaTileEntityBlock
        get() = blockState.block as NovaTileEntityBlock
    
    /**
     * The [FakeItemDisplay(s)][FakeItemDisplay] used to display the model of this [TileEntity], if it is entity-backed.
     */
    val displayEntities: List<FakeItemDisplay>?
        get() = DisplayEntityBlockModelProvider.entities[pos]
    
    /**
     * Whether this [TileEntity] is enabled.
     */
    var isEnabled: Boolean = false
        internal set
    
    private val dropProviders = ArrayList<() -> Collection<ItemStack>>()
    lateinit var menuContainer: MenuContainer
        private set
    
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
    
    /**
     * Called when this [TileEntity] is placed.
     */
    open fun handlePlace(ctx: Context<BlockPlace>) = Unit
    
    /**
     * Called when this [TileEntity] is destroyed.
     */
    open fun handleBreak(ctx: Context<BlockBreak>) = Unit
    
    /**
     * Called when this [TileEntity] is enabled.
     *
     * May not add or remove any [TileEntities][TileEntity].
     */
    open fun handleEnable() = Unit
    
    /**
     * Called when this [TileEntity] is disabled.
     *
     * May not add or remove any [TileEntities][TileEntity].
     */
    open fun handleDisable() {
        if (::menuContainer.isInitialized)
            menuContainer.closeWindows()
    }
    
    /**
     * Called every tick for every TileEntity that is in a loaded chunk.
     */
    open fun handleTick() = Unit
    
    /**
     * Called asynchronously for every tick that this TileEntity is in a loaded chunk.
     */
    open suspend fun handleAsyncTick() = Unit
    
    /**
     * Called when a [TileEntityBlockBehavior] is interacted with.
     *
     * Might be called twice, once per hand.
     *
     * @return If any action was performed.
     */
    open fun handleRightClick(ctx: Context<BlockInteract>): Boolean {
        val player = ctx[ContextParamTypes.SOURCE_ENTITY] as? Player
            ?: return false
        
        if (::menuContainer.isInitialized && !player.hasInventoryOpen) {
            menuContainer.openWindow(player)
            return true
        }
        
        return false
    }
    
    /**
     * Gets a list of [ItemStacks][ItemStack] to be dropped when this [TileEntity] is destroyed.
     */
    open fun getDrops(includeSelf: Boolean): List<ItemStack> {
        val drops = ArrayList<ItemStack>()
        if (includeSelf) {
            saveData()
            
            val item = block.item?.createItemStack()
            if (item != null) {
                if (persistentData.isNotEmpty()) {
                    item.novaCompound[TILE_ENTITY_DATA_KEY] = persistentData
                }
                
                drops += item
            }
        }
        
        for (dropProvider in dropProviders)
            drops += dropProvider()
        
        return drops
    }
    
    /**
     * Gets the amount of exp to be dropped when this [TileEntity] is destroyed.
     */
    open fun getExp(): Int = 0
    
    /**
     * Called to save all data using the [storeData] method.
     */
    open fun saveData() {
        saveDataAccessors()
    }
    
    /**
     * Gets a [List] of all [players][Player] that this [TileEntity] is
     * visible for.
     */
    fun getViewers(): List<Player> =
        FakeEntityManager.getChunkViewers(pos.chunkPos)
    
    /**
     * Changes the block state of this [TileEntity] to [blockState].
     *
     * @throws IllegalArgumentException If [blockState] is not of this [TileEntity's][TileEntity] block type.
     */
    fun updateBlockState(blockState: NovaBlockState) {
        require(isEnabled) { "TileEntity needs to be enabled" }
        require(blockState.block == block) { "New block state needs to be of the same block type" }
        
        val prevBlockState = this.blockState
        if (blockState == prevBlockState)
            return
        
        blockState.modelProvider.replace(pos, prevBlockState.modelProvider)
        WorldDataManager.setBlockState(pos, blockState)
    }
    
    /**
     * Registers a new drop provider that will be called during [TileEntity.getDrops] when this [TileEntity] is destroyed.
     */
    fun dropProvider(provider: () -> Collection<ItemStack>) {
        dropProviders += provider
    }
    
    /**
     * Retrieves a [VirtualInventory] stored under [name] or creates a new one using the specified
     * [size], [maxStackSizes] and [persistent] properties. Then registers the specified [postUpdateHandler]
     * and [preUpdateHandler].
     *
     * If the inventory is not [persistent], it will also be registered as a drop provider, i.e. it's contents
     * will be dropped when the [TileEntity] is destroyed.
     *
     * Note that [size] and [maxStackSizes] are only used when creating a new inventory, which means that the
     * inventory retrieved may not necessarily be of that size or have those max stack sizes.
     */
    fun storedInventory(
        name: String,
        size: Int,
        persistent: Boolean = false,
        maxStackSizes: IntArray = IntArray(size) { 64 },
        preUpdateHandler: ((ItemPreUpdateEvent) -> Unit)? = null,
        postUpdateHandler: ((ItemPostUpdateEvent) -> Unit)? = null,
    ): VirtualInventory {
        // legacy conversion
        val legacyName = "inventory.${uuid.salt(name)}"
        if (hasData(legacyName)) {
            storeData(name, retrieveDataOrNull<VirtualInventory>(legacyName)!!)
            removeData(legacyName)
        }
        
        val inventory = storedValue(name, persistent) {
            VirtualInventory(UUID.nameUUIDFromBytes(name.toByteArray()), size, null, maxStackSizes)
        }.get()
        
        if (preUpdateHandler != null)
            inventory.setPreUpdateHandler(preUpdateHandler)
        if (postUpdateHandler != null)
            inventory.setPostUpdateHandler(postUpdateHandler)
        if (!persistent)
            dropProvider { inventory.items.filterNotNull() }
        
        return inventory
    }
    
    /**
     * Retrieves a [VirtualInventory] stored under [name] or creates a new one using the specified [size].
     * Then registers the specified [postUpdateHandler] and [preUpdateHandler].
     *
     * The inventory will also be registered as a drop provider, i.e. it's contents will be dropped when the
     * [TileEntity] is destroyed.
     *
     * Note that [size] is only used when creating a new inventory, which means that the inventory retrieved may
     * not necessarily be of that size.
     */
    fun storedInventory(
        name: String,
        size: Int,
        preUpdateHandler: ((ItemPreUpdateEvent) -> Unit)? = null,
        postUpdateHandler: ((ItemPostUpdateEvent) -> Unit)? = null,
    ): VirtualInventory = storedInventory(name, size, persistent = false, preUpdateHandler = preUpdateHandler, postUpdateHandler = postUpdateHandler)
    
    fun storedFluidContainer(
        name: String,
        allowedTypes: Set<FluidType>,
        capacity: Provider<Long>,
        persistent: Boolean = false,
        updateHandler: (() -> Unit)? = null,
    ): DynamicFluidContainer {
        val uuid = UUID.nameUUIDFromBytes(name.toByteArray())
        
        // legacy conversion
        val legacyName = "fluidContainer.$uuid"
        var legacyAmount: Long = 0
        var legacyType: FluidType? = null
        if (hasData(legacyName)) {
            val legacyData = retrieveDataOrNull<Compound>(legacyName)!!
            legacyAmount = legacyData["amount"]!!
            legacyType = legacyData["type"]
            removeData(legacyName)
        }
        
        val container = DynamicFluidContainer(
            uuid,
            allowedTypes,
            storedValue("${name}_type", persistent) { legacyType },
            capacity,
            storedValue("${name}_value", persistent) { legacyAmount },
        )
        
        if (updateHandler != null)
            container.addUpdateHandler(updateHandler)
        
        return container
    }
    
    fun storedRegion(
        name: String,
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): DynamicRegion {
        val legacyName = "region.$name"
        var size = defaultSize
        if (hasData(legacyName)) {
            size = retrieveDataOrNull<Int>(legacyName)!!
            removeData(legacyName)    
        }
        
        return DynamicRegion(
            uuid.salt(name),
            minSize, maxSize,
            storedValue(name) { size },
            createRegion
        )
    }
    
    override fun toString(): String {
        return "${javaClass.simpleName}(blockState=$blockState, pos=$pos, data=$data)"
    }
    
    abstract inner class TileEntityMenu internal constructor(protected val texture: GuiTexture? = null) {
        
        open fun getTitle(): Component =
            texture?.getTitle(block.name) ?: block.name
        
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
