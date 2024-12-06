package xyz.xenondevs.nova.resources.builder.layout.item

import org.bukkit.DyeColor
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.HeadKind
import xyz.xenondevs.nova.resources.builder.data.Orientation
import xyz.xenondevs.nova.resources.builder.data.SpecialItemModel
import xyz.xenondevs.nova.resources.builder.data.SpecialItemModel.SpecialModel
import xyz.xenondevs.nova.resources.builder.data.WoodType
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.util.RequiredProperty

@RegistryElementBuilderDsl
abstract class SpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    /**
     * A function that selects the base model, which will be used for transformations, particle texture, GUI light, etc.
     */
    var base: S.() -> ModelBuilder = { getModel("minecraft:block/block") }
    
    internal abstract fun build(): SpecialItemModel
    
}

@RegistryElementBuilderDsl
class BedSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The texture of the bed, in `textures/entity/bed/`.
     */
    var texture: ResourcePath<ResourceType.BedTexture> by RequiredProperty("Bed texture is undefined")
    
    override fun build() = SpecialItemModel(
        SpecialModel.Bed(texture),
        selectAndBuild(base)
    )
    
}

@RegistryElementBuilderDsl
class BannerSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The color of the banner base.
     */
    var color: DyeColor = DyeColor.WHITE
    
    override fun build() = SpecialItemModel(
        SpecialModel.Banner(color),
        selectAndBuild(base)
    )
    
}

@RegistryElementBuilderDsl
class ChestSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The texture of the chest, in `textures/entity/chest/`.
     */
    var texture: ResourcePath<ResourceType.ChestTexture> by RequiredProperty("Chest texture is undefined")
    
    /**
     * How far the chest model is opened, in `[0, 1]`.
     */
    var openness: Double = 0.0
    
    override fun build() = SpecialItemModel(
        SpecialModel.Chest(texture, openness),
        selectAndBuild(base)
    )
    
}

@RegistryElementBuilderDsl
class HeadSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The type of head model.
     */
    var kind: HeadKind = HeadKind.PLAYER
    
    /**
     * The texture of the head, in `textures/entity/`.
     */
    var texture: ResourcePath<ResourceType.EntityTexture> by RequiredProperty("Head texture is undefined")
    
    /**
     * The animation stage of the head, in `[0, 1]`.
     */
    var animation: Double = 0.0
    
    override fun build() = SpecialItemModel(
        SpecialModel.Head(kind, texture, animation),
        selectAndBuild(base)
    )
    
}

@RegistryElementBuilderDsl
class ShulkerBoxSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The texture of the shulker box, in `textures/entity/shulker/`.
     */
    var texture: ResourcePath<ResourceType.ShulkerTexture> by RequiredProperty("Shulker box texture is undefined")
    
    /**
     * How far the shulker box model is opened, in `[0, 1]`.
     */
    var openness: Double = 0.0
    
    /**
     * The orientation of the shulker box model.
     */
    var orientation: Orientation = Orientation.UP
    
    override fun build() = SpecialItemModel(
        SpecialModel.ShulkerBox(texture, openness, orientation),
        selectAndBuild(base)
    )
    
}

@RegistryElementBuilderDsl
class SignSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    private val make: (WoodType, ResourcePath<ResourceType.SignTexture>?) -> SpecialModel,
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The type of wood the sign is made of. Ignored if [texture] is set.
     */
    var woodType: WoodType? = null
    
    /**
     * The texture of the sign, in `textures/entity/signs`. Overrides [woodType].
     */
    var texture: ResourcePath<ResourceType.SignTexture>? = null
    
    override fun build(): SpecialItemModel {
        require(woodType != null || texture != null) { "Either wood type or texture needs to be defined in sign special model" }
        
        return SpecialItemModel(
            make(woodType ?: WoodType.OAK, texture),
            selectAndBuild(base)
        )
    }
    
}

@RegistryElementBuilderDsl
class GenericSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    private val model: SpecialModel,
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    override fun build() = SpecialItemModel(model, selectAndBuild(base))
    
}