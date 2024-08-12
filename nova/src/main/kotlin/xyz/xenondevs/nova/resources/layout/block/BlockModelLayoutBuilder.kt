package xyz.xenondevs.nova.resources.layout.block

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.layout.block.BlockModelLayout.LayoutType
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty

private typealias BlockStateSelector = BlockSelectorScope.() -> BlockData
private typealias BlockModelSelector = BlockModelSelectorScope.() -> ModelBuilder

private val DEFAULT_STATE_SELECTOR: BlockStateSelector = { Material.BARRIER.createBlockData() }
private val DEFAULT_MODEL_SELECTOR: BlockModelSelector = { defaultModel }

internal class BlockModelLayout(
    val type: LayoutType,
    val priority: Int,
    val backingStateConfigTypes: List<BackingStateConfigType<*>>,
    val stateSelector: BlockStateSelector,
    val modelSelector: BlockModelSelector
) {
    
    companion object {
        val DEFAULT = BlockModelLayout(LayoutType.ENTITY_BACKED, 0, emptyList(), DEFAULT_STATE_SELECTOR, DEFAULT_MODEL_SELECTOR)
    }
    
    enum class LayoutType {
        STATE_BACKED, ENTITY_BACKED, MODEL_LESS
    }
    
}

@RegistryElementBuilderDsl
class BlockModelLayoutBuilder internal constructor() {
    
    private var layoutType: LayoutType = LayoutType.ENTITY_BACKED
    private var backingStatePriority = 0
    private var backingStateConfigTypes: List<BackingStateConfigType<*>>? = null
    private var stateSelector: BlockStateSelector? = null
    private var modelSelector: BlockModelSelector? = null
    
    /**
     * Configures the backing state types of this block model.
     *
     * State-backed custom block models are more performant than entity-backed models, but have some limitations.
     * There is also only a certain amount of total block states available that can be used for custom block models.
     *
     * If no more backing states are available at runtime, the entity-backed display mode will be used instead.
     */
    fun stateBacked(category: BackingStateCategory, vararg other: BackingStateCategory) {
        stateBacked(0, category, *other)
    }
    
    /**
     * Configures the backing state types of this block model.
     *
     * State-backed custom block models are more performant than entity-backed models, but have some limitations.
     * There is also only a certain amount of total block states available that can be used for custom block models.
     *
     * If no more backing states are available at runtime, the entity-backed display mode will be used instead.
     *
     * The [priority] value determines the order in which backing states are distributed to registered blocks, where
     * blocks with a higher priority value will be assigned a backing state first.
     * As a general guideline, the priority should be an estimation of how many blocks of this type will be in a chunk.
     */
    fun stateBacked(priority: Int, category: BackingStateCategory, vararg other: BackingStateCategory) {
        layoutType = LayoutType.STATE_BACKED
        backingStatePriority = priority
        backingStateConfigTypes = listOf(category, *other).flatMap { it.backingStateConfigTypes }
        stateSelector = { category.fallbackHitbox }
    }
    
    /**
     * Configures the hitbox type of this entity-based block model.
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
     */
    fun entityBacked(stateSelector: BlockStateSelector = DEFAULT_STATE_SELECTOR) {
        layoutType = LayoutType.ENTITY_BACKED
        this.stateSelector = stateSelector
    }
    
    /**
     * Configures this block to not use any custom models, but instead use the given [stateSelector].
     */
    fun modelLess(stateSelector: BlockStateSelector) {
        layoutType = LayoutType.MODEL_LESS
        this.stateSelector = stateSelector
    }
    
    /**
     * Configures the models based on the block state.
     */
    fun selectModel(modelSelector: BlockModelSelector) {
        this.modelSelector = modelSelector
    }
    
    /**
     * Configures the models based on the given [property].
     */
    @JvmName("selectModelString")
    fun <T : Any> selectModel(property: BlockStateProperty<T>, vararg models: Pair<T, String>) {
        selectModel(property, models.toMap(HashMap()))
    }
    
    /**
     * Configures the models based on the given [property].
     */
    @JvmName("selectModelString")
    fun <T : Any> selectModel(property: BlockStateProperty<T>, models: Map<T, String>) {
        selectModel {
            val state = getPropertyValueOrNull(property)
            models[state]?.let(::getModel) ?: defaultModel
        }
    }
    
    /**
     * Configures the models based on the given [property].
     */
    @JvmName("selectModelResourcePath")
    fun <T : Any> selectModel(property: BlockStateProperty<T>, vararg models: Pair<T, ResourcePath>) {
        selectModel(property, models.toMap(HashMap()))
    }
    
    /**
     * Configures the models based on the given [property].
     */
    @JvmName("selectModelResourcePath")
    fun <T : Any> selectModel(property: BlockStateProperty<T>, models: Map<T, ResourcePath>) {
        selectModel {
            val state = getPropertyValueOrNull(property)
            models[state]?.let(::getModel) ?: defaultModel
        }
    }
    
    internal fun build(): BlockModelLayout = when (layoutType) {
        LayoutType.STATE_BACKED -> BlockModelLayout(
            LayoutType.STATE_BACKED,
            backingStatePriority,
            backingStateConfigTypes!!,
            stateSelector ?: DEFAULT_STATE_SELECTOR,
            modelSelector ?: DEFAULT_MODEL_SELECTOR,
        )
        
        LayoutType.ENTITY_BACKED -> BlockModelLayout(
            LayoutType.ENTITY_BACKED,
            0,
            emptyList(),
            stateSelector ?: DEFAULT_STATE_SELECTOR,
            modelSelector ?: DEFAULT_MODEL_SELECTOR,
        )
        
        LayoutType.MODEL_LESS -> BlockModelLayout(
            LayoutType.MODEL_LESS,
            0,
            emptyList(),
            stateSelector ?: DEFAULT_STATE_SELECTOR,
            DEFAULT_MODEL_SELECTOR,
        )
    }
    
}