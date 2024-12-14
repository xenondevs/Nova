package xyz.xenondevs.nova.resources.builder.layout.item

import org.joml.Vector3d
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BundleSelectedItemItemModel
import xyz.xenondevs.nova.resources.builder.data.EmptyItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModelDefinition
import xyz.xenondevs.nova.resources.builder.data.SpecialItemModel.SpecialModel
import xyz.xenondevs.nova.resources.builder.data.TintSource
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.ui.menu.Canvas
import java.awt.Color

@RegistryElementBuilderDsl
class ItemModelDefinitionBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The model of this item. Empty by default.
     */
    var model: ItemModel = empty()
    
    /**
     * Whether a down-and-up animation should be played in first person view when the
     * item is changed (either type, count, components or by swapping the item into the other hand)
     */
    var handAnimationOnSwap = true
    
    internal fun build() = ItemModelDefinition(model, handAnimationOnSwap)
    
    internal companion object {
        
        // it is not possible to just use the default model as default value, as that would result in
        // selectAndBuild running for them, which is not desired (ModelContent would remember the models and keep them, even if unused)
        val DEFAULT_CONFIGURE_ITEM_MODEL_SELECTOR: ItemModelDefinitionBuilder<ItemModelSelectorScope>.() -> Unit = { model = buildModel { defaultModel } }
        val DEFAULT_CONFIGURE_BLOCK_MODEL_SELECTOR: ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit = { model = buildModel { defaultModel } }
        
    }
    
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
    
    /**
     * Creates a canvas model, which is an item model with a flat texture where each pixel is individually
     * addressable using the `colors` part of the `minecraft:custom_model_data` component, where
     * the pixel at (x, y) is found under the index `y * width + x` and (0, 0) is the top-left pixel.
     *
     * The maximum [width] and [height] are `161 - |offsetX|` and `161 - |offsetY|` respectively.
     * (Consider using multiple smaller canvases instead of a single large one to reduce the resource pack size.)
     * 
     * @param width The width of the canvas, in pixels.
     * @param height The height of the canvas, in pixels.
     * @param offsetX The x offset of the canvas texture to the item's center, pixels.
     * @param offsetY The y offset of the canvas texture to the item's center, pixels.
     * @param scale The size of the pixels in the canvas texture.
     * A scale of 2 means each pixel is 2x2 pixels in game (assuming a client-side gui-scale of 1).
     * Defaults to 1.
     * 
     * @see Canvas
     */
    fun canvasModel(
        width: Int, height: Int,
        offsetX: Double = 0.0, offsetY: Double = 0.0,
        scale: Double = 1.0
    ): ItemModel = select(SelectItemModelProperty.DisplayContext) {
        // the actual width and height of the canvas, in pixel models needed, takes scale into account
        val actualWidth = (width / scale).toInt()
        val actualHeight = (height / scale).toInt()
        
        // the individual pixel models apply a display scale of 4, so actualScale counteracts this with 0.25
        val actualScale = 0.25 * scale
        
        // parent model for all pixels with this scale
        val parentModel = Model(
            parent = ResourcePath(ResourceType.Model, "nova", "item/canvas_pixel"),
            elements = listOf(
                Model.Element(
                    from = Vector3d(8.0, 8.0 - actualScale, 0.0),
                    to = Vector3d(8.0 + actualScale, 8.0, 0.0),
                    faces = enumMapOf(
                        Model.Direction.SOUTH to Model.Element.Face(
                            texture = "#0",
                            tintIndex = 0
                        )
                    )
                )
            )
        )
        val parentModelId = resourcePackBuilder.getHolder<ModelContent>().getOrPutGenerated(parentModel)
        
        fallback = empty()
        case[DisplayContext.GUI] = composite {
            for (y in 0..<actualHeight) {
                for (x in 0..<actualWidth) {
                    val i = y * actualWidth + x
                    models += model {
                        tintSource[0] = TintSource.CustomModelData(Color.WHITE, i)
                        model = {
                            ModelBuilder(Model(
                                parent = parentModelId,
                                display = mapOf(
                                    Model.Display.Position.GUI to Model.Display(
                                        scale = Vector3d(4.0, 4.0, 4.0),
                                        translation = Vector3d(
                                            (width / -2.0) + offsetX + x * scale,
                                            -((height / -2.0) + offsetY + y * scale),
                                            0.0
                                        )
                                    )
                                )
                            ))
                        }
                    }
                }
            }
        }
    }
    
}