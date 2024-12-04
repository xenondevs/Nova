@file:Suppress("unused")

package xyz.xenondevs.nova.world.block

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.ConfigurableRegistryElementBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelector
import xyz.xenondevs.nova.resources.builder.layout.block.BlockStateSelector
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_STATE_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_ENTITY_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.ItemDefinitionConfigurator
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

abstract class AbstractNovaBlockBuilder<B : NovaBlock> internal constructor(
    id: ResourceLocation
) : ConfigurableRegistryElementBuilder<B>(NovaRegistries.BLOCK, id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    protected var style: Style = Style.empty()
    protected var name: Component = Component.translatable("block.${id.namespace}.${id.path}")
    protected var behaviors = ArrayList<BlockBehaviorHolder>()
    protected val stateProperties = ArrayList<ScopedBlockStateProperty<*>>()
    internal var layout: BlockModelLayout = BlockModelLayout.DEFAULT
    
    /**
     * Sets the style of the block name.
     */
    fun style(style: Style) {
        this.style = style
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(color: TextColor) {
        this.style = Style.style(color)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(color: TextColor, vararg decorations: TextDecoration) {
        this.style = Style.style(color, *decorations)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(vararg decorations: TextDecoration) {
        this.style = Style.style(*decorations)
    }
    
    /**
     * Sets the style of the block name.
     */
    fun style(decoration: TextDecoration) {
        this.style = Style.style(decoration)
    }
    
    /**
     * Sets the name of the block.
     *
     * This function is exclusive with [localizedName].
     */
    fun name(name: Component) {
        this.name = name
    }
    
    /**
     * Sets the localization key of the block.
     *
     * Defaults to `block.<namespace>.<name>`.
     *
     * This function is exclusive with [name].
     */
    fun localizedName(localizedName: String) {
        this.name = Component.translatable(localizedName)
    }
    
    /**
     * Sets the behaviors of this block to [behaviors].
     */
    open fun behaviors(vararg behaviors: BlockBehaviorHolder) {
        this.behaviors += behaviors
    }
    
    /**
     * Adds the [stateProperties] to the properties of this block.
     */
    fun stateProperties(vararg stateProperties: ScopedBlockStateProperty<*>) {
        this.stateProperties += stateProperties
    }
    
    /**
     * Configures the backing state types of this block model via the given ([category], [categories]),
     * then selects the corresponding block models via the given [modelSelector].
     *
     * State-backed custom block models are more performant than entity-backed models, but have some limitations.
     * There is also only a certain amount of total block states available that can be used for custom block models.
     *
     * If no more backing states are available at runtime, the entity-backed display mode will be used instead.
     *
     * Exclusive with [entityBacked] and [modelLess].
     */
    fun stateBacked(
        category: BackingStateCategory, vararg categories: BackingStateCategory,
        modelSelector: BlockModelSelector = DEFAULT_BLOCK_MODEL_SELECTOR
    ) = stateBacked(0, category, *categories, modelSelector = modelSelector)
    
    /**
     * Configures the backing state types of this block model via the given ([category], [categories]),
     * then selects the corresponding block models via the given [modelSelector].
     *
     * State-backed custom block models are more performant than entity-backed models, but have some limitations.
     * There is also only a certain amount of total block states available that can be used for custom block models.
     *
     * If no more backing states are available at runtime, the entity-backed display mode will be used instead.
     *
     * The [priority] value determines the order in which backing states are distributed to registered blocks, where
     * blocks with a higher priority value will be assigned a backing state first.
     * As a general guideline, the priority should be an estimation of how many blocks of this type will be in a chunk.
     *
     * Exclusive with [entityBacked] and [modelLess].
     */
    fun stateBacked(
        priority: Int,
        category: BackingStateCategory, vararg categories: BackingStateCategory,
        modelSelector: BlockModelSelector = DEFAULT_BLOCK_MODEL_SELECTOR
    ) {
        layout = BlockModelLayout.StateBacked(
            priority,
            listOf(category, *categories).flatMap { it.backingStateConfigTypes },
            modelSelector
        )
    }
    
    /**
     * Configures the model and hitbox type of this entity-based block model via [stateSelector] and [modelSelector] respectively.
     *
     * Entity-backed custom block models are less performant than state-backed models, but a lot more flexible:
     *
     * * They can display transparent- and oversized (larger than 3x3x3) models.
     * * There is no limit to the amount of different models.
     * * Every vanilla block type can be used as a hitbox (a block inside the display entity).
     *   This allows for very customizable colliders.
     * * The item display entities can be accessed and updated at runtime.
     *
     * Note that only tile-entity blocks can use entity-backed models.
     *
     * Exclusive with [stateBacked] and [modelLess].
     */
    fun entityBacked(
        stateSelector: BlockStateSelector = DEFAULT_BLOCK_STATE_SELECTOR,
        modelSelector: BlockModelSelector = DEFAULT_BLOCK_MODEL_SELECTOR
    ) {
        layout = BlockModelLayout.SimpleEntityBacked(stateSelector, modelSelector)
    }
    
    /**
     * Configures the model and hitbox type of this entity-based block model via [stateSelector] and [itemSelector] respectively.
     * 
     * Entity-backed custom block models based on custom item definitions are less performant than state-backed models, but a lot more flexible.
     * In contrast to [entityBacked], models defined via custom item definitions cannot benefit from display entity transformations, as some
     * selection functionality is client-side only. As such, oversized models are not supported.
     *
     * Feature list:
     * * They can take advantage of 1.21.4's item model definition system, which makes it possible to use special model
     *   types such as chest or signs.
     * * They can display transparent models
     * * There is no limit to the amount of different models
     * * Every vanilla block type can be used as a hitbox (a block inside the display entity)
     *   This allows for very customizable colliders
     * * The item display entities can be accessed and updated at runtime.
     */
    fun entityItemBacked(
        stateSelector: BlockStateSelector = DEFAULT_BLOCK_STATE_SELECTOR,
        itemSelector: ItemDefinitionConfigurator = DEFAULT_ENTITY_BLOCK_MODEL_SELECTOR
    ) {
        layout = BlockModelLayout.ItemEntityBacked(stateSelector, itemSelector)
    }
    
    /**
     * Configures this block to not use any custom models, but instead use the given [stateSelector].
     *
     * Exclusive with [stateBacked] and [entityBacked].
     */
    fun modelLess(stateSelector: BlockStateSelector) {
        layout = BlockModelLayout.ModelLess(stateSelector)
    }
    
}

class NovaBlockBuilder internal constructor(id: ResourceLocation) : AbstractNovaBlockBuilder<NovaBlock>(id) {
    
    internal constructor(addon: Addon, name: String) : this(ResourceLocation(addon, name))
    
    override fun build() = NovaBlock(
        id,
        name.style(style),
        style,
        behaviors,
        stateProperties,
        configId,
        layout
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
    
    /**
     * Configures the amount of times [TileEntity.handleTick] is called per second.
     * Accepts values from 0 to 20, with 0 disabling ticking.
     *
     * Defaults to 20.
     */
    fun tickrate(tickrate: Int) {
        require(tickrate in 0..20) { "Sync TPS must be between 0 and 20" }
        this.syncTickrate = tickrate
    }
    
    override fun build() = NovaTileEntityBlock(
        id,
        name.style(style),
        style,
        behaviors,
        tileEntity,
        syncTickrate,
        stateProperties,
        configId,
        layout
    )
    
}