package xyz.xenondevs.nova.registry

import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelector
import xyz.xenondevs.nova.resources.builder.layout.block.BlockStateSelector
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_STATE_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.ItemDefinitionConfigurator
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder.Companion.DEFAULT_CONFIGURE_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.behavior.BlockDrops
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * A builder for [NovaBlock].
 */
@RegistryElementBuilderDsl
sealed interface NovaBlockBuilder : ConfigurableBuilder, NameableBuilder, RegistryEntryBuilder.Nova<NovaBlock> {
    
    /**
     * Sets the item type of this block. Used in, for example, block drops via [BlockDrops].
     * 
     * If this is not set, defaults to the [NovaItem] that used this block to create it's [NovaItemBuilder] (in [Registrar.item]).
     * Note that this only applies if the block is passed directly in the [Registrar.item] function, NOT if the block is defined via [NovaItemBuilder.block].
     */
    fun item(item: RegistryEntry.Nova<NovaItem>)
    
    /**
     * Sets the behaviors of this block to [behaviors].
     */
    fun behaviors(vararg behaviors: BlockBehaviorHolder)
    
    /**
     * Adds the [stateProperties] to the properties of this block.
     */
    fun stateProperties(vararg stateProperties: ScopedBlockStateProperty<*>)
    
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
    )
    
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
     * Exclusive with [stateBacked] and [modelLess].
     */
    fun entityBacked(
        stateSelector: BlockStateSelector = DEFAULT_BLOCK_STATE_SELECTOR,
        modelSelector: BlockModelSelector = DEFAULT_BLOCK_MODEL_SELECTOR
    )
    
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
        itemSelector: ItemDefinitionConfigurator = DEFAULT_CONFIGURE_BLOCK_MODEL_SELECTOR
    )
    
    /**
     * Configures this block to not use any custom models, but instead use the given [stateSelector].
     *
     * Exclusive with [stateBacked] and [entityBacked].
     */
    fun modelLess(stateSelector: BlockStateSelector)
    
}