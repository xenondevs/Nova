package xyz.xenondevs.nova.resources.builder.layout.item

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BundleSelectedItemItemModel
import xyz.xenondevs.nova.resources.builder.data.EmptyItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModelDefinition
import xyz.xenondevs.nova.resources.builder.data.SpecialItemModel.SpecialModel
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder

@RegistryElementBuilderDsl
class ItemModelDefinitionBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The model of this item.
     */
    var model: ItemModel = empty()
    
    /**
     * Whether a down-and-up animation should be played in first person view when the
     * item is changed (either type, count, components or by swapping the item into the other hand)
     */
    var handAnimationOnSwap = true
    
    internal fun build() = ItemModelDefinition(model, handAnimationOnSwap)
    
}

@RegistryElementBuilderDsl
sealed class ItemModelCreationScope<S : ModelSelectorScope>(
    val resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) {
    
    /**
     * Renders a plain model.
     */
    fun model(model: ItemModelBuilder<S>.() -> Unit): ItemModel =
        ItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(model).build()
    
    /**
     * Renders multiple models in the same space.
     */
    fun composite(compositeModel: CompositeItemModelBuilder<S>.() -> Unit): ItemModel =
        CompositeItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(compositeModel).build()
    
    /**
     * Chooses a model based on a binary condition of type [property].
     */
    fun condition(
        property: ConditionItemModelProperty,
        conditionModel: ConditionItemModelBuilder<S>.() -> Unit
    ): ItemModel = ConditionItemModelBuilder(property, resourcePackBuilder, selectAndBuild).apply(conditionModel).build()
    
    /**
     * Selects a model based on the value of [property].
     */
    fun <T> select(
        property: SelectItemModelProperty<T>,
        selectModel: SelectItemModelBuilder<T, S>.() -> Unit
    ): ItemModel = SelectItemModelBuilder(property, resourcePackBuilder, selectAndBuild).apply(selectModel).build()
    
    /**
     * Selects a model based on the value of [property], by selecting the last entry
     * that is less or equal than a defined value.
     */
    fun rangeDispatch(
        property: RangeDispatchItemModelProperty,
        rangeDispatchModel: RangeDispatchItemModelBuilder<S>.() -> Unit
    ): ItemModel = RangeDispatchItemModelBuilder(property, resourcePackBuilder, selectAndBuild).apply(rangeDispatchModel).build()
    
    /**
     * Renders the selected item stack of the `minecraft:bundle_contents` component if present, otherwise does nothing.
     */
    fun bundleSelectedItem(): ItemModel = BundleSelectedItemItemModel
    
    /**
     * Renders nothing.
     */
    fun empty(): ItemModel = EmptyItemModel
    
    // --- Special Models ---
    
    /**
     * Renders a bed model.
     */
    fun bedSpecialModel(bedModel: BedSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        BedSpecialItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(bedModel).build()
    
    /**
     * Renders a banner model.
     */
    fun bannerSpecialModel(bannerModel: BannerSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        BannerSpecialItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(bannerModel).build()
    
    /**
     * Renders a conduit model.
     */
    fun conduitSpecialModel(conduitModel: GenericSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        GenericSpecialItemModelBuilder(SpecialModel.Conduit, resourcePackBuilder, selectAndBuild).apply(conduitModel).build()
    
    /**
     * Renders a chest model.
     */
    fun chestSpecialModel(chestModel: ChestSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        ChestSpecialItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(chestModel).build()
    
    /**
     * Renders a decorated pot model.
     */
    fun decoratedPotSpecialModel(decoratedPotModel: GenericSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        GenericSpecialItemModelBuilder(SpecialModel.DecoratedPot, resourcePackBuilder, selectAndBuild).apply(decoratedPotModel).build()
    
    /**
     * Renders a head model.
     */
    fun headSpecialModel(headModel: HeadSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        HeadSpecialItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(headModel).build()
    
    /**
     * Renders a shulker box model.
     */
    fun shulkerBoxSpecialModel(shulkerBoxModel: ShulkerBoxSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        ShulkerBoxSpecialItemModelBuilder(resourcePackBuilder, selectAndBuild).apply(shulkerBoxModel).build()
    
    /**
     * Renders a shield model.
     */
    fun shieldSpecialModel(shieldModel: GenericSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        GenericSpecialItemModelBuilder(SpecialModel.Shield, resourcePackBuilder, selectAndBuild).apply(shieldModel).build()
    
    /**
     * Renders a standing sign model.
     */
    fun standingSignSpecialModel(standingSignModel: SignSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        SignSpecialItemModelBuilder(SpecialModel::StandingSign, resourcePackBuilder, selectAndBuild).apply(standingSignModel).build()
    
    /**
     * Renders a trident model.
     */
    fun tridentSpecialModel(tridentModel: GenericSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        GenericSpecialItemModelBuilder(SpecialModel.Trident, resourcePackBuilder, selectAndBuild).apply(tridentModel).build()
    
    /**
     * Renders a hanging sign model.
     */
    fun hangingSignSpecialModel(hangingSignModel: SignSpecialItemModelBuilder<S>.() -> Unit): ItemModel =
        SignSpecialItemModelBuilder(SpecialModel::HangingSign, resourcePackBuilder, selectAndBuild).apply(hangingSignModel).build()
    
    // --- Utility Functions ---
    
    /**
     * Creates a plain model from [selectModel].
     * This is equivalent to `model { model = selectModel }`.
     */
    fun buildModel(selectModel: S.() -> ModelBuilder): ItemModel =
        model { model = selectModel }
    
    /**
     * Creates a [rangeDispatch] model with cases for each number in the given [range], controlled by the
     * value at [index] in `floats` of the `minecraft:custom_model_data` component.
     */
    fun numberedModels(range: IntRange, index: Int = 0, selectModel: S.(Int) -> ModelBuilder): ItemModel =
        rangeDispatch(RangeDispatchItemModelProperty.CustomModelData(index)) {
            for (i in range) {
                entry[i] = { selectModel(i) }
            }
        }
    
    /**
     * Creates a [rangeDispatch] model with [count] cases from 0 to 1, controlled by the value at [index] in
     * `floats` of the `minecraft:custom_model_data` component.
     */
    fun rangedModels(count: Int, index: Int = 0, selectModel: S.(Int) -> ModelBuilder): ItemModel =
        rangeDispatch(RangeDispatchItemModelProperty.CustomModelData(index)) {
            repeat(count) { i ->
                entry[i.toDouble() / (count - 1).toDouble()] = { selectModel(i) }
            }
        }
    
}