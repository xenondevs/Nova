package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.feline.CatVariant
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftCat
import org.bukkit.entity.Cat
import xyz.xenondevs.nova.resources.builder.layout.entity.AgingEntityVariantLayoutBuilder
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.util.toIdentifier

class CatVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Cat.Type, Unit, CatVariant, EntityVariantLayout.Aging, AgingEntityVariantLayoutBuilder>(
    Registries.CAT_VARIANT,
    CraftCat.CraftType::minecraftHolderToBukkit,
    Unit,
    ::AgingEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Aging, spawnConditions: SpawnPrioritySelectors) =
        CatVariant(
            ClientAsset.ResourceTexture(layout.adultTexture.toIdentifier()),
            ClientAsset.ResourceTexture(layout.babyTexture.toIdentifier()),
            spawnConditions,
        )
    
}