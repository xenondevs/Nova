package xyz.xenondevs.nova.registry

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import org.bukkit.Color
import org.bukkit.block.BlockType
import org.bukkit.block.PistonMoveReaction
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.block.BlockSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_STATE_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_EXTRA_COLLIDER_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder.Companion.DEFAULT_CONFIGURE_BLOCK_MODEL_SELECTOR
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.world.block.ColliderCube
import xyz.xenondevs.nova.world.block.FluidFlowMode
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.behavior.BlockDrops
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.item.behavior.Tool

/**
 * A builder for [NovaBlock].
 */
@RegistryElementBuilderDsl
sealed interface NovaBlockBuilder : ConfigurableBuilder, NameableBuilder, RegistryEntryBuilder.Paper<BlockType> {
    
    /**
     * Sets the item type of this block. Used in, for example, block drops via [BlockDrops].
     * 
     * If this is not set, defaults to the [ItemType] that used this block to create it's [NovaItemBuilder] (in [Registrar.item]).
     * Note that this only applies if the block is passed directly in the [Registrar.item] function, NOT if the block is defined via [NovaItemBuilder.block].
     */
    fun item(item: RegistryEntry.Paper<ItemType>)
    
    /**
     * Adds to the behaviors of this block to [behaviors].
     */
    fun behaviors(vararg behaviors: BlockBehaviorHolder)
    
    /**
     * Adds the [stateProperties] to the properties of this block.
     */
    fun stateProperties(vararg stateProperties: BlockStateProperty<*>)
    
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
        modelSelector: BlockModelSelectorScope.() -> ModelBuilder = DEFAULT_BLOCK_MODEL_SELECTOR
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
        modelSelector: BlockModelSelectorScope.() -> ModelBuilder = DEFAULT_BLOCK_MODEL_SELECTOR
    )
    
    /**
     * Configures the model and hitbox type of this entity-based block model via [modelSelector], [stateSelector] and [extraColliderSelector] respectively.
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
        stateSelector: BlockSelectorScope.() -> BlockData = DEFAULT_BLOCK_STATE_SELECTOR,
        extraColliderSelector: BlockSelectorScope.() -> List<ColliderCube> = DEFAULT_EXTRA_COLLIDER_SELECTOR,
        modelSelector: BlockModelSelectorScope.() -> ModelBuilder = DEFAULT_BLOCK_MODEL_SELECTOR
    )
    
    /**
     * Configures the model and hitbox/collider of this entity-based block model via [itemSelector], [stateSelector] and [extraColliderSelector] respectively.
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
        stateSelector: BlockSelectorScope.() -> BlockData = DEFAULT_BLOCK_STATE_SELECTOR,
        extraColliderSelector: BlockSelectorScope.() -> List<ColliderCube> = DEFAULT_EXTRA_COLLIDER_SELECTOR,
        itemSelector: ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit = DEFAULT_CONFIGURE_BLOCK_MODEL_SELECTOR
    )
    
    /**
     * Configures this block to not use any custom models, but instead use the given [stateSelector].
     *
     * Exclusive with [stateBacked] and [entityBacked].
     */
    fun modelLess(stateSelector: BlockSelectorScope.() -> BlockData)
    
    /**
     * Makes this block breakable. Higher [hardness] values increase the time required to break it,
     * while [requiresToolForDrops] determines whether an appropriate tool is needed for drops.
     *
     * The actual tool category / tool tier behavior is defined on the item-side via the [tool component][DataComponentTypes.TOOL] / [tool behavior][Tool].
     * The convention is to add your block to the appropriate `<namespace>:mineable/<category>` tag for the tool category, and to the appropriate
     * `<namespace>:needs_<tier>_tool` tag for the tool tier.
     * [toolCategories] and [toolTier] add the block to these corresponding tags automatically via [tags], but you can also omit
     * them here and add the tags manually.
     * The vanilla `sword` and `shears` categories instead use the `minecraft:sword_efficient` and
     * `minecraft:shears_major_breaking_speed` tags, respectively.
     * The lowest vanilla harvest tiers, `wood`, `wooden`, and `gold`, do not create a `needs_*_tool` tag.
     * 
     * [hitParticles] and [breakParticles] are used while breaking entity-backed blocks that use barriers.
     * Since all blocks can fall back to entity-backed models, these should still be set for state-backed blocks.
     * 
     * Independently, [showBreakAnimation] can be used to disable the breaking animation entirely.
     */
    fun breakable(
        hardness: Double = 1.0,
        toolCategories: Set<Key> = emptySet(),
        toolTier: Key? = null,
        requiresToolForDrops: Boolean = false,
        hitParticles: RegistryEntry.Paper<ItemType>? = null,
        breakParticles: RegistryEntry.Paper<BlockType>? = null,
        showBreakAnimation: Boolean = true
    )
    
    /**
     * Configures the sounds made by this block.
     * Defaults to [SoundGroup.EMPTY].
     */
    fun sounds(soundGroup: SoundGroup)
    
    /**
     * Configures how pistons interact with this block.
     * Defaults to [PistonMoveReaction.MOVE].
     * Tile Entities can never be moved.
     */
    fun pistonReaction(pistonReaction: PistonMoveReaction)
    
    /**
     * Configures the light level emitted by all states of this block.
     * Must be between `0` and `15`.
     */
    fun lightEmission(level: Int) = lightEmission { level }
    
    /**
     * Configures the emitted light level for each block state.
     * Selected values must be between `0` and `15`.
     */
    fun lightEmission(selectLightEmission: BlockSelectorScope.() -> Int)
    
    /**
     * Configures this block's resistance to explosions. Higher values make the block harder to destroy.
     */
    fun explosionResistance(explosionResistance: Float)
    
    /**
     * Configures how this block interacts with vanilla fire.
     *
     * [igniteOdds] controls how easily fire spreads into nearby air, while [burnOdds] controls
     * how easily existing fire consumes this block. These values are weights, not percentages.
     * [ignitedByLava] controls whether lava can ignite fire next to this block.
     *
     * For reference, vanilla uses `5, 5` for logs, `5, 20` for planks, `30, 60` for leaves,
     * and `60, 100` for grass and flowers.
     */
    fun flammable(igniteOdds: Int, burnOdds: Int, ignitedByLava: Boolean)
    
    /**
     * Configures this block's color on maps.
     * The closest color available in Minecraft's map palette is used.
     */
    fun mapColor(mapColor: Color)
    
    /**
     * Configures how fluids flow into and out of all states of this block.
     *
     * Defaults to [FluidFlowMode.BLOCK].
     */
    fun fluidFlowMode(mode: FluidFlowMode) = fluidFlowMode { mode }
    
    /**
     * Configures how fluids flow into and out of each block state.
     *
     * Defaults to [FluidFlowMode.BLOCK].
     */
    fun fluidFlowMode(selectFluidFlowMode: BlockSelectorScope.() -> FluidFlowMode)
    
}
