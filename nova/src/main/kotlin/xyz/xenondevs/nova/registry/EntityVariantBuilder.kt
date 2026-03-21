package xyz.xenondevs.nova.registry

import net.minecraft.world.entity.animal.chicken.ChickenVariant
import net.minecraft.world.entity.animal.cow.CowVariant
import net.minecraft.world.entity.animal.pig.PigVariant
import xyz.xenondevs.nova.resources.builder.layout.entity.AgingEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.WolfEntityVariantLayoutBuilder

/**
 * A builder for an entity variant.
 * 
 * @param M Model type enum, or [Unit] if there is only one model type.
 * @param LB Layout builder type.
 */
@RegistryElementBuilderDsl
sealed interface EntityVariantBuilder<M : Any, LB : EntityVariantLayoutBuilder<*>> {
    
    /**
     * Configures the [conditions](https://minecraft.wiki/w/Mob_variant_definitions#Spawn_condition) under which this entity variant can spawn.
     *
     * When spawning, the selection of a variant happens in 3 steps:
     * 1. Each condition is evaluated. The matching condition with the highest priority determines the priority of the variant. If no condition matches the variant is discarded.
     * 2. Of all remaining variants, all except those with the highest priority are discarded.
     * 3. Of the remaining variants, a random variant is chosen.
     */
    fun spawnConditions(spawnConditions: SpawnConditionsBuilder.() -> Unit)
    
    /**
     * Configures the [modelType] and [texture] of the entity variant.
     */
    fun texture(modelType: M, texture: LB.() -> Unit)
    
    /**
     * Configures the [texture] of the entity variant using the default model type.
     */
    fun texture(texture: LB.() -> Unit)
    
}

/**
 * A builder for a cat variant.
 */
sealed interface CatVariantBuilder : EntityVariantBuilder<Unit, AgingEntityVariantLayoutBuilder>

/**
 * A builder for a chicken variant.
 */
sealed interface ChickenVariantBuilder : EntityVariantBuilder<ChickenModelType, AgingEntityVariantLayoutBuilder>

/**
 * A builder for a cow variant.
 */
sealed interface CowVariantBuilder : EntityVariantBuilder<CowModelType, AgingEntityVariantLayoutBuilder>

/**
 * A builder for a frog variant.
 */
sealed interface FrogVariantBuilder : EntityVariantBuilder<Unit, SimpleEntityVariantLayoutBuilder>

/**
 * A builder for a pig variant.
 */
sealed interface PigVariantBuilder : EntityVariantBuilder<PigModelType, AgingEntityVariantLayoutBuilder>

/**
 * A builder for a wolf variant.
 */
sealed interface WolfVariantBuilder : EntityVariantBuilder<Unit, WolfEntityVariantLayoutBuilder>

/**
 * The type of chicken model.
 */
enum class ChickenModelType(internal val nms: ChickenVariant.ModelType) {
    
    /**
     * The model that the normal (temperate) chicken uses.
     * ![](https://i.imgur.com/63VHbVu.png)
     */
    NORMAL(ChickenVariant.ModelType.NORMAL),
    
    /**
     * The model that the cold chicken uses.
     * ![](https://i.imgur.com/diwvo2i.png)
     */
    COLD(ChickenVariant.ModelType.COLD),
    
}

/**
 * The type of cow model.
 */
enum class CowModelType(internal val nms: CowVariant.ModelType) {
    
    /**
     * The model that the normal (temperate) cow uses.
     * ![](https://i.imgur.com/iO4TgKJ.png)
     */
    NORMAL(CowVariant.ModelType.NORMAL),
    
    /**
     * The model that the cold cow uses.
     * ![](https://i.imgur.com/kFM6tl5.png)
     */
    COLD(CowVariant.ModelType.COLD),
    
    /**
     * The model that the warm cow uses.
     * ![](https://i.imgur.com/WHgcTW9.png)
     */
    WARM(CowVariant.ModelType.WARM),
    
}

/**
 * The type of pig model.
 */
enum class PigModelType(internal val nms: PigVariant.ModelType) {
    
    /**
     * The model the normal (temperate) pig uses.
     * ![](https://i.imgur.com/O9yGmfT.png)
     */
    NORMAL(PigVariant.ModelType.NORMAL),
    
    /**
     * The model the cold pig uses.
     * ![](https://i.imgur.com/OIcwNIV.png)
     */
    COLD(PigVariant.ModelType.COLD)
    
}