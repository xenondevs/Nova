package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import net.minecraft.core.ClientAsset
import net.minecraft.resources.RegistryOps
import net.minecraft.world.entity.animal.chicken.ChickenVariant
import net.minecraft.world.entity.animal.cow.CowVariant
import net.minecraft.world.entity.animal.feline.CatVariant
import net.minecraft.world.entity.animal.frog.FrogVariant
import net.minecraft.world.entity.animal.pig.PigVariant
import net.minecraft.world.entity.animal.wolf.WolfVariant
import net.minecraft.world.entity.variant.ModelAndTexture
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.Keyed
import org.bukkit.entity.Cat
import org.bukkit.entity.Chicken
import org.bukkit.entity.Cow
import org.bukkit.entity.Frog
import org.bukkit.entity.Pig
import org.bukkit.entity.Wolf
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.AgingEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.WolfEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.EntityVariantTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.toIdentifier

internal abstract class AbstractEntityVariantBuilder<T : Keyed, NMS : Any, L : EntityVariantLayout, M : Any, LB : EntityVariantLayoutBuilder<L>>(
    override val entry: RegistryEntry.Paper<T>,
    private val defaultModelType: M,
    private val makeLayoutBuilder: (namespace: String, ResourcePackBuilder) -> LB,
    private val registryKey: RegistryKey<*>
) : EntityVariantBuilder<T, M, LB>, RegistryElementBuilder.Vanilla<NMS> {
    
    private val id = entry.key
    private var configureSpawnConditions: (SpawnConditionsBuilder.() -> Unit)? = null
    private var modelType: M = defaultModelType
    
    override fun spawnConditions(spawnConditions: SpawnConditionsBuilder.() -> Unit) {
        this.configureSpawnConditions = spawnConditions
    }
    
    override fun texture(modelType: M, texture: LB.() -> Unit) {
        this.modelType = modelType
        EntityVariantTask.queueVariantAssetGeneration(registryKey.key(), id) {
            makeLayoutBuilder(id.namespace(), it)
                .apply(texture)
                .build()
        }
    }
    
    override fun texture(texture: LB.() -> Unit) = texture(defaultModelType, texture)
    
    internal abstract fun build(modelType: M, layout: L, spawnConditions: SpawnPrioritySelectors): NMS
    
    @Suppress("UNCHECKED_CAST")
    override fun build(lookup: RegistryOps.RegistryInfoLookup): NMS {
        val layout = ResourceLookups.entityVariantAssets[registryKey.key() to id]
            ?: throw IllegalStateException("Missing variant assets for $id in lookup")
        val spawnConditions = configureSpawnConditions?.let { SpawnConditionsBuilderImpl(lookup).apply(it).build() }
        val variant = build(modelType, layout as L, spawnConditions ?: SpawnPrioritySelectors.EMPTY)
        return variant
    }
    
}

internal class CatVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Cat.Type>,
) : AbstractEntityVariantBuilder<Cat.Type, CatVariant, EntityVariantLayout.Aging, Unit, AgingEntityVariantLayoutBuilder>(
    entry,
    Unit,
    ::AgingEntityVariantLayoutBuilder,
    RegistryKey.CAT_VARIANT
), CatVariantBuilder {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Aging, spawnConditions: SpawnPrioritySelectors) =
        CatVariant(
            ClientAsset.ResourceTexture(layout.adultTexture.toIdentifier()),
            ClientAsset.ResourceTexture(layout.babyTexture.toIdentifier()),
            spawnConditions,
        )
    
}

internal class ChickenVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Chicken.Variant>,
) : AbstractEntityVariantBuilder<Chicken.Variant, ChickenVariant, EntityVariantLayout.Aging, ChickenModelType, AgingEntityVariantLayoutBuilder>(
    entry,
    ChickenModelType.NORMAL,
    ::AgingEntityVariantLayoutBuilder,
    RegistryKey.CHICKEN_VARIANT
), ChickenVariantBuilder {
    
    override fun build(modelType: ChickenModelType, layout: EntityVariantLayout.Aging, spawnConditions: SpawnPrioritySelectors) =
        ChickenVariant(
            ModelAndTexture(modelType.nms, layout.adultTexture.toIdentifier()),
            ClientAsset.ResourceTexture(layout.babyTexture.toIdentifier()),
            spawnConditions,
        )
    
}

internal class CowVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Cow.Variant>,
) : AbstractEntityVariantBuilder<Cow.Variant, CowVariant, EntityVariantLayout.Aging, CowModelType, AgingEntityVariantLayoutBuilder>(
    entry,
    CowModelType.NORMAL,
    ::AgingEntityVariantLayoutBuilder,
    RegistryKey.COW_VARIANT
), CowVariantBuilder {
    
    override fun build(modelType: CowModelType, layout: EntityVariantLayout.Aging, spawnConditions: SpawnPrioritySelectors) =
        CowVariant(
            ModelAndTexture(modelType.nms, layout.adultTexture.toIdentifier()),
            ClientAsset.ResourceTexture(layout.babyTexture.toIdentifier()),
            spawnConditions,
        )
    
}

internal class FrogVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Frog.Variant>
) : AbstractEntityVariantBuilder<Frog.Variant, FrogVariant, EntityVariantLayout.Simple, Unit, SimpleEntityVariantLayoutBuilder>(
    entry,
    Unit,
    ::SimpleEntityVariantLayoutBuilder,
    RegistryKey.FROG_VARIANT
), FrogVariantBuilder {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        FrogVariant(ClientAsset.ResourceTexture(layout.texture.toIdentifier()), spawnConditions)
    
}

internal class PigVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Pig.Variant>
) : AbstractEntityVariantBuilder<Pig.Variant, PigVariant, EntityVariantLayout.Aging, PigModelType, AgingEntityVariantLayoutBuilder>(
    entry,
    PigModelType.NORMAL,
    ::AgingEntityVariantLayoutBuilder,
    RegistryKey.PIG_VARIANT
), PigVariantBuilder {
    
    override fun build(modelType: PigModelType, layout: EntityVariantLayout.Aging, spawnConditions: SpawnPrioritySelectors) =
        PigVariant(
            ModelAndTexture(modelType.nms, layout.adultTexture.toIdentifier()),
            ClientAsset.ResourceTexture(layout.babyTexture.toIdentifier()),
            spawnConditions,
        )
    
}

internal class WolfVariantBuilderImpl internal constructor(
    entry: RegistryEntry.Paper<Wolf.Variant>
) : AbstractEntityVariantBuilder<Wolf.Variant, WolfVariant, EntityVariantLayout.Wolf, Unit, WolfEntityVariantLayoutBuilder>(
    entry,
    Unit,
    ::WolfEntityVariantLayoutBuilder,
    RegistryKey.WOLF_VARIANT
), WolfVariantBuilder {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Wolf, spawnConditions: SpawnPrioritySelectors): WolfVariant =
        WolfVariant(
            layout.adultTextures.toAssetInfo(),
            layout.babyTextures.toAssetInfo(),
            spawnConditions,
        )
    
    private fun EntityVariantLayout.WolfTextureSet.toAssetInfo() = WolfVariant.AssetInfo(
        ClientAsset.ResourceTexture(wild.toIdentifier()),
        ClientAsset.ResourceTexture(tame.toIdentifier()),
        ClientAsset.ResourceTexture(angry.toIdentifier()),
    )
    
}
