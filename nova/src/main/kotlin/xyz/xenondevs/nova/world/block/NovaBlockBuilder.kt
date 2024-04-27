@file:Suppress("unused")

package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.data.resources.layout.block.BlockModelLayoutBuilder
import xyz.xenondevs.nova.item.NovaMaterialTypeRegistryElementBuilder
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.behavior.InteractiveTileEntityBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.TileEntityBlockBehavior
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty

private val EMPTY_BLOCK_OPTIONS = BlockOptions(0.0)

abstract class AbstractNovaBlockBuilder<B : NovaBlock> internal constructor(
    id: ResourceLocation
) : NovaMaterialTypeRegistryElementBuilder<B>(NovaRegistries.BLOCK, id, "block.${id.namespace}.${id.name}") {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    protected abstract var behaviors: MutableList<BlockBehavior>
    protected val stateProperties = ArrayList<ScopedBlockStateProperty<*>>()
    protected var options: BlockOptions = EMPTY_BLOCK_OPTIONS
    internal var requestedLayout = BlockModelLayout.DEFAULT
    
    /**
     * Sets the behaviors of this block to [behaviors].
     */
    open fun behaviors(vararg behaviors: BlockBehavior) {
        this.behaviors = behaviors.toMutableList()
    }
    
    /**
     * Adds the [behaviors] to the behaviors of this block.
     */
    open fun addBehaviors(vararg block: BlockBehavior) {
        this.behaviors += block
    }
    
    /**
     * Adds the [stateProperties] to the properties of this block.
     */
    fun stateProperties(vararg stateProperties: ScopedBlockStateProperty<*>) {
        this.stateProperties += stateProperties
    }
    
    /**
     * Sets the [BlockOptions] of this block.
     */
    fun blockOptions(options: BlockOptions) {
        this.options = options
    }
    
    fun models(buildModel: BlockModelLayoutBuilder.() -> Unit) {
        val builder = BlockModelLayoutBuilder()
        builder.buildModel()
        requestedLayout = builder.build()
    }
    
}

class NovaBlockBuilder internal constructor(id: ResourceLocation) : AbstractNovaBlockBuilder<NovaBlock>(id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    override var behaviors: MutableList<BlockBehavior> = mutableListOf(BlockBehavior.Default)
    
    override fun build() = NovaBlock(
        id,
        name.style(style),
        style,
        behaviors,
        options,
        stateProperties,
        configId,
        requestedLayout
    )
    
}

class NovaTileEntityBlockBuilder internal constructor(
    id: ResourceLocation,
    private val tileEntity: TileEntityConstructor
) : AbstractNovaBlockBuilder<NovaTileEntityBlock>(id) {
    
    private var syncTickrate: Int = 20
    private var asyncTickrate: Double = 0.0
    
    internal constructor(
        addon: Addon,
        name: String,
        tileEntity: TileEntityConstructor
    ) : this(ResourceLocation(addon, name), tileEntity)
    
    override var behaviors: MutableList<BlockBehavior> = mutableListOf(InteractiveTileEntityBlockBehavior)
    
    /**
     * Configures whether this tile-entity is interactive, i.e. if it can be right-clicked.
     * 
     * Defaults to `true`.
     */
    fun interactive(interactive: Boolean) {
        behaviors.removeIf { it is TileEntityBlockBehavior }
        behaviors += if (interactive) InteractiveTileEntityBlockBehavior else TileEntityBlockBehavior
    }
    
    /**
     * Configures the amount of times [TileEntity.handleTick] is called per second.
     * Accepts values from 0 to 20, with 0 disabling sync ticking.
     * 
     * Defaults to 20.
     */
    fun syncTickrate(syncTickrate: Int) {
        require(syncTickrate in 0..20) { "Sync TPS must be between 0 and 20" }
        this.syncTickrate = syncTickrate
    }
    
    /**
     * Configures the amount of times [TileEntity.handleAsyncTick] is called per second.
     * Accepts any value >= 0.0, with 0 disabling async ticking.
     * 
     * Defaults to 0 (disabled).
     */
    fun asyncTickrate(asyncTickrate: Double) {
        require(asyncTickrate >= 0) { "Async TPS must be greater than or equal to 0" }
        this.asyncTickrate = asyncTickrate
    }
    
    override fun build() = NovaTileEntityBlock(
        id,
        name.style(style),
        style,
        behaviors,
        options,
        tileEntity,
        syncTickrate, asyncTickrate,
        stateProperties,
        configId,
        requestedLayout
    )
    
}