package xyz.xenondevs.nova.world.entity

import net.kyori.adventure.key.Key
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.CatVariant
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftCat
import org.bukkit.entity.Cat
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toResourceLocation

class CatVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Cat.Type, Unit, CatVariant, EntityVariantLayout.Simple, SimpleEntityVariantLayoutBuilder>(
    Registries.CAT_VARIANT,
    CraftCat.CraftType::minecraftHolderToBukkit,
    Unit,
    ::SimpleEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        CatVariant(ClientAsset(layout.texture.toResourceLocation()), spawnConditions)
    
}