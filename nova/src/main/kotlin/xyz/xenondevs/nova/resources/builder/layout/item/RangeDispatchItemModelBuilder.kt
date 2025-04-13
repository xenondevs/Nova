@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.layout.item

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.CompassTarget
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.TimeSource
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder

/**
 * A collection of range dispatch entries.
 */
class RangeDispatchEntries<S : ModelSelectorScope> internal constructor(
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) {
    
    internal val entries = ArrayList<ItemModel.RangeDispatch.Entry>()
    
    /**
     * Adds an entry that displays the model created by [selectModel] when the property value
     * is equal or greater than [threshold] and there is no closer entry.
     */
    operator fun set(threshold: Number, selectModel: S.() -> ModelBuilder) {
        set(threshold, ItemModel.Default(selectAndBuild(selectModel)))
    }
    
    /**
     * Adds an entry that displays [model] when the property value is equal or greater than [threshold]
     * and there is no closer entry.
     */
    operator fun set(threshold: Number, model: ItemModel) {
        entries += ItemModel.RangeDispatch.Entry(threshold.toDouble(), model)
    }
    
}

@RegistryElementBuilderDsl
class RangeDispatchItemModelBuilder<S : ModelSelectorScope> internal constructor(
    private val property: RangeDispatchItemModelProperty,
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The entries for this range dispatch item model.
     */
    val entry = RangeDispatchEntries<S>(selectAndBuild)
    
    /**
     * Multiplier for property values.
     */
    var scale: Double = 1.0
    
    /**
     * The fallback model if no entry matches.
     * Can be `null`, but will render a "missing" error model instead.
     */
    var fallback: ItemModel? = null
    
    internal fun build(): ItemModel.RangeDispatch =
        property.buildModel(scale, entry.entries, fallback)
    
}

sealed class RangeDispatchItemModelProperty(internal val property: ItemModel.RangeDispatch.Property) {
    
    internal open fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
        ItemModel.RangeDispatch(property, scale, entries, fallback)
    
    /**
     * Returns the weight of the `minecraft:bundle_contents` component, or `0` if not present.
     */
    object BundleFullness : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.BUNDLE_FULLNESS)
    
    /**
     * Returns the value of the `minecraft:damage` component, or `0` if not present.
     * - If [normalize] is `true`, the count is divided by the value of `minecraft:max_stack_size` and clamped in  `[0, 1]`.
     * - If [normalize] is `false`, the count is clamped in `[0, infinity[`.
     */
    open class Damage(
        private val normalize: Boolean
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.DAMAGE) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, normalize = normalize)
        
        companion object : Damage(true)
        
    }
    
    /**
     * Returns item stack's count.
     * - If [normalize] is `true`, the count is divided by the value of `minecraft:max_stack_size` and clamped in  `[0, 1]`.
     * - If [normalize] is `false`, the count is clamped in `[0, infinity[`.
     */
    open class Count(
        private val normalize: Boolean
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.COUNT) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, normalize = normalize)
        
        companion object : Count(true)
        
    }
    
    /**
     * Returns the remaining cooldown for the item, scaled between 0 and 1.
     */
    object Cooldown : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.COOLDOWN)
    
    /**
     * Returns the in-game time of [source], scaled between 0 and 1.
     * If [wobble] is `true`, the value will oscillate for some time around the target before settling.
     */
    class Time(
        private val source: TimeSource,
        private val wobble: Boolean = true
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.TIME) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, source = source, wobble = wobble)
        
    }
    
    /**
     * Returns an angle in the x-z plane between the holder and target positions, scaled from 0 to 1.
     * If the target is not valid (i.e. not present, in another dimension or too close to the holder position),
     * a random value will be returned.
     */
    class Compass(
        private val target: CompassTarget,
        private val wobble: Boolean = true
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.COMPASS) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, target = target, wobble = wobble)
        
    }
    
    /**
     * Returns the crossbow pull progress, scaled between 0 and 1.
     */
    object CrossbowPull : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.CROSSBOW_PULL)
    
    /**
     * Returns item use ticks.
     * - If [remaining] is `true`, the returned value will be the remaining use ticks.
     * - If [remaining] is `false`, the returned value will be the ticks so far.
     */
    open class UseDuration(
        private val remaining: Boolean
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.USE_DURATION) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, remaining = remaining)
        
        companion object : UseDuration(false)
        
    }
    
    /**
     * Returns the remaining item use ticks modulo [period].
     */
    open class UseCycle(
        private val period: Double
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.USE_CYCLE) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, period = period)
        
        companion object : UseCycle(0.0)
        
    }
    
    /**
     * Returns the value at [index] from `floats` of the `minecraft:custom_model_data` component.
     */
    open class CustomModelData(
        private val index: Int
    ) : RangeDispatchItemModelProperty(ItemModel.RangeDispatch.Property.CUSTOM_MODEL_DATA) {
        
        override fun buildModel(scale: Double, entries: List<ItemModel.RangeDispatch.Entry>, fallback: ItemModel?) =
            ItemModel.RangeDispatch(property, scale, entries, fallback, index = index)
        
        companion object : CustomModelData(0)
        
    }
    
}