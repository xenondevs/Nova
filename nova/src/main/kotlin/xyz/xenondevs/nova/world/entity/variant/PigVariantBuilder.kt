package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.PigVariant
import net.minecraft.world.entity.variant.ModelAndTexture
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftPig
import org.bukkit.entity.Pig
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toResourceLocation

class PigVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Pig.Variant, PigModelType, PigVariant, EntityVariantLayout.Simple, SimpleEntityVariantLayoutBuilder>(
    Registries.PIG_VARIANT,
    CraftPig.CraftVariant::minecraftHolderToBukkit,
    PigModelType.NORMAL,
    ::SimpleEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: PigModelType, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        PigVariant(ModelAndTexture(modelType.nms, layout.texture.toResourceLocation()), spawnConditions)
    
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