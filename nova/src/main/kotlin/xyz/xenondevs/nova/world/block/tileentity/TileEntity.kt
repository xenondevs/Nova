@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.world.block.tileentity

import kotlinx.coroutines.Job
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.serialization.DataHolder
import xyz.xenondevs.nova.util.item.storeData
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.BlockUpdateMethod
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.FluidContainer
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.createItemStack
import xyz.xenondevs.nova.world.region.DynamicRegion
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*

/**
 * A custom tile entity.
 */
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
        val TILE_ENTITY_DATA_KEY = Key.key("nova", "tileentity")
        
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
    
    /**
     * This [TileEntity's][TileEntity] menu.
     * Also tracks other [Windows][Window] that belong to this [TileEntity].
     */
    open val menu: TileEntityMenu = TileEntityMenu.none()
    
    /**
     * The supervisor [Job] for coroutines of this [TileEntity].
     *
     * Will be available for use after ticking was enabled, indicated by [handleEnableTicking].
     * Will be automatically cancelled when ticking is disabled, indicated by [handleDisableTicking].
     */
    lateinit var coroutineSupervisor: Job
        internal set
    
    private val dropProviders = ArrayList<() -> Collection<ItemStack>>()
    private val disableHandlers = ArrayList<() -> Unit>()
    
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
        menu.close()
        disableHandlers.forEach { it() }
    }
    
    /**
     * Called when this [TileEntity] starts ticking.
     *
     * May not add or remove any [TileEntities][TileEntity].
     */
    open fun handleEnableTicking() = Unit
    
    /**
     * Called when this [TileEntity] stops ticking.
     *
     * May not add or remove any [TileEntities][TileEntity].
     */
    open fun handleDisableTicking() = Unit
    
    /**
     * Called every tick for every TileEntity that is in a loaded chunk.
     */
    open fun handleTick() = Unit
    
    /**
     * @see BlockBehavior.useItemOn
     */
    open fun useItemOn(ctx: Context<BlockInteract>): InteractionResult {
        return InteractionResult.Pass
    }
    
    /**
     * @see BlockBehavior.use
     */
    open fun use(ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_ENTITY] as? Player
            ?: return InteractionResult.Pass
        if (menu.open(player))
            return InteractionResult.Success(swing = true)
        return InteractionResult.Pass
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
                    item.storeData(TILE_ENTITY_DATA_KEY, persistentData)
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
    open fun saveData() = Unit
    
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
    fun updateBlockState(blockState: NovaBlockState, method: BlockUpdateMethod = BlockUpdateMethod.WITH_BLOCK_UPDATES) {
        require(isEnabled) { "TileEntity needs to be enabled" }
        require(blockState.block == block) { "New block state needs to be of the same block type" }
        
        val prevBlockState = this.blockState
        if (blockState == prevBlockState)
            return
        
        blockState.modelProvider.replace(pos, prevBlockState.modelProvider, method)
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
        val inventory = storedValue(name, persistent) {
            VirtualInventory(UUID.nameUUIDFromBytes(name.toByteArray()), size, null, maxStackSizes)
        }.get()
        
        if (preUpdateHandler != null)
            inventory.addPreUpdateHandler(preUpdateHandler)
        if (postUpdateHandler != null)
            inventory.addPostUpdateHandler(postUpdateHandler)
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
    
    /**
     * Retrieves a [FluidContainer] stored under [name] or creates an empty new one.
     * The values [allowedTypes] and [capacity] are not serialized and will be
     * applied every time.
     * Then registers the specified [updateHandler].
     *
     * If [persistent] is true, the container will be stored in the item when the [TileEntity] is dropped.
     */
    fun storedFluidContainer(
        name: String,
        allowedTypes: Set<FluidType>,
        capacity: Provider<Long>,
        persistent: Boolean = false,
        updateHandler: (() -> Unit)? = null,
    ): FluidContainer {
        val uuid = UUID.nameUUIDFromBytes(name.toByteArray())
        
        val container = FluidContainer(
            storedValue(name, persistent, ::Compound),
            uuid,
            allowedTypes,
            capacity,
        )
        
        if (updateHandler != null)
            container.addUpdateHandler(updateHandler)
        
        return container
    }
    
    /**
     * Creates a [DynamicRegion] with the size stored under [name] or [defaultSize] if it doesn't exist, using [createRegion].
     * The [minSize] and [maxSize] properties are not serialized and will be applied every time.
     */
    fun storedRegion(
        name: String,
        minSize: Provider<Int>,
        maxSize: Provider<Int>,
        defaultSize: Int,
        createRegion: (Int) -> Region
    ): DynamicRegion {
        val regionUuid = uuid.salt(name)
        disableHandlers += { VisualRegion.removeRegion(regionUuid) }
        return DynamicRegion(
            regionUuid,
            minSize, maxSize,
            storedValue(name) { defaultSize },
            createRegion
        )
    }
    
    override fun toString(): String {
        return "${javaClass.simpleName}(blockState=$blockState, pos=$pos, data=$data)"
    }
    
}
