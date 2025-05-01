package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.ChickenVariant
import net.minecraft.world.entity.variant.ModelAndTexture
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftChicken
import org.bukkit.entity.Chicken
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toResourceLocation

class ChickenVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Chicken.Variant, ChickenModelType, ChickenVariant, EntityVariantLayout.Simple, SimpleEntityVariantLayoutBuilder>(
    Registries.CHICKEN_VARIANT,
    CraftChicken.CraftVariant::minecraftHolderToBukkit,
    ChickenModelType.NORMAL,
    ::SimpleEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: ChickenModelType, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        ChickenVariant(ModelAndTexture(modelType.nms, layout.texture.toResourceLocation()), spawnConditions)
    
}

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