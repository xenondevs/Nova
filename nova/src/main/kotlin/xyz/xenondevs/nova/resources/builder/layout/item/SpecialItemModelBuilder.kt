@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.layout.item

import org.bukkit.DyeColor
import org.joml.Matrix4f
import org.joml.Matrix4fc
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BannerAttachment
import xyz.xenondevs.nova.resources.builder.data.BedPart
import xyz.xenondevs.nova.resources.builder.data.ChestType
import xyz.xenondevs.nova.resources.builder.data.CopperGolemStatuePose
import xyz.xenondevs.nova.resources.builder.data.EndCubeEffect
import xyz.xenondevs.nova.resources.builder.data.HangingSignAttachment
import xyz.xenondevs.nova.resources.builder.data.HeadKind
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModel.Special.SpecialModel
import xyz.xenondevs.nova.resources.builder.data.StandingSignAttachment
import xyz.xenondevs.nova.resources.builder.data.WoodType
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.util.RequiredProperty

@RegistryElementBuilderDsl
abstract class SpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    val resourcePackBuilder: ResourcePackBuilder
) {
    
    /**
     * A function that selects the base model, which will be used for transformationations, particle texture, GUI light, etc.
     */
    var base: S.() -> ModelBuilder = { getModel("minecraft:block/block") }
    
    /**
     * An additional transformationation matrix that is applied to the special model on top of the
     * transformationations defined in the [base] model.
     */
    var transformation: Matrix4fc = Matrix4f()
    
    internal abstract fun build(): ItemModel.Special
    
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
    
    /**
     * Where the banner is attached.
     */
    var attachment: BannerAttachment = BannerAttachment.GROUND
    
    override fun build() = ItemModel.Special(
        SpecialModel.Banner(color, attachment),
        selectAndBuild(base),
        transformation
    )
    
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
    
    /**
     * Defines which half of the bed that is rendered.
     */
    var part: BedPart = BedPart.HEAD
    
    override fun build() = ItemModel.Special(
        SpecialModel.Bed(texture, part),
        selectAndBuild(base),
        transformation
    )
    
}

@RegistryElementBuilderDsl
class BookSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * Angle (in degrees) between the book cover and the centerline (`0` means closed, `90` means open flat).
     */
    var openAngle: Int by RequiredProperty("Book open angle is undefined")
    
    /**
     * The position of the first page inside the book.
     * `0` means the page is completely on the left side, `1` means it's completely on the right side.
     */
    var page1: Float by RequiredProperty("Book page 1 angle is undefined")
    
    /**
     * The position of the second page inside the book.
     * `0` means the page is completely on the left side, `1` means it's completely on the right side.
     */
    var page2: Float by RequiredProperty("Book page 2 angle is undefined")
    
    override fun build() = ItemModel.Special(
        SpecialModel.Book(openAngle, page1, page2),
        selectAndBuild(base),
        transformation
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
    
    /**
     * The type of the chest model.
     */
    var type: ChestType = ChestType.SINGLE
    
    override fun build() = ItemModel.Special(
        SpecialModel.Chest(texture, openness, type),
        selectAndBuild(base),
        transformation
    )
    
}

@RegistryElementBuilderDsl
class CopperGolemStatueSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The pose that the statue takes.
     */
    var pose: CopperGolemStatuePose = CopperGolemStatuePose.STANDING
    
    /**
     * The texture of the statue, in `textures/`.
     */
    var texture by RequiredProperty<ResourcePath<ResourceType.CopperGolemStatueTexture>>("Copper golem statue texture is undefined")
    
    override fun build() = ItemModel.Special(
        SpecialModel.CopperGolemStatue(pose, texture),
        selectAndBuild(base),
        transformation
    )
    
}

@RegistryElementBuilderDsl
class EndCubeSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    /**
     * The texture effect of the cube.
     */
    var effect: EndCubeEffect = EndCubeEffect.PORTAL
    
    override fun build() = ItemModel.Special(
        SpecialModel.EndCube(effect),
        selectAndBuild(base),
        transformation
    )
    
}


@RegistryElementBuilderDsl
class HangingSignSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
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
    
    /**
     * Where the hanging sign is attached.
     */
    var attachment: HangingSignAttachment = HangingSignAttachment.CEILING_MIDDLE
    
    override fun build(): ItemModel.Special {
        require(woodType != null || texture != null) { "Either wood type or texture needs to be defined in hanging sign special model" }
        
        return ItemModel.Special(
            SpecialModel.HangingSign(woodType ?: WoodType.OAK, texture, attachment),
            selectAndBuild(base),
            transformation
        )
    }
    
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
    
    override fun build() = ItemModel.Special(
        SpecialModel.Head(kind, texture, animation),
        selectAndBuild(base),
        transformation
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
    
    override fun build() = ItemModel.Special(
        SpecialModel.ShulkerBox(texture, openness),
        selectAndBuild(base),
        transformation
    )
    
}

@RegistryElementBuilderDsl
class StandingSignSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
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
    
    /**
     * Where the standing sign is attached.
     */
    var attachment: StandingSignAttachment = StandingSignAttachment.GROUND
    
    override fun build(): ItemModel.Special {
        require(woodType != null || texture != null) { "Either wood type or texture needs to be defined in standing sign special model" }
        
        return ItemModel.Special(
            SpecialModel.StandingSign(woodType ?: WoodType.OAK, texture, attachment),
            selectAndBuild(base),
            transformation
        )
    }
    
}

@RegistryElementBuilderDsl
class GenericSpecialItemModelBuilder<S : ModelSelectorScope> internal constructor(
    private val model: SpecialModel,
    resourcePackBuilder: ResourcePackBuilder,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : SpecialItemModelBuilder<S>(resourcePackBuilder) {
    
    override fun build() = ItemModel.Special(model, selectAndBuild(base), transformation)
    
}