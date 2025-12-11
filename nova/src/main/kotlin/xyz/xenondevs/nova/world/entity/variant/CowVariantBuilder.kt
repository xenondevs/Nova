package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.cow.CowVariant
import net.minecraft.world.entity.variant.ModelAndTexture
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftCow
import org.bukkit.entity.Cow
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toIdentifier

class CowVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Cow.Variant, CowModelType, CowVariant, EntityVariantLayout.Simple, SimpleEntityVariantLayoutBuilder>(
    Registries.COW_VARIANT,
    CraftCow.CraftVariant::minecraftHolderToBukkit,
    CowModelType.NORMAL,
    ::SimpleEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: CowModelType, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        CowVariant(ModelAndTexture(modelType.nms, layout.texture.toIdentifier()), spawnConditions)
    
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