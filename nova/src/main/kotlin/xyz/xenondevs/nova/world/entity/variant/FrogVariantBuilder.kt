package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.frog.FrogVariant
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftFrog
import org.bukkit.entity.Frog
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.SimpleEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toIdentifier

class FrogVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Frog.Variant, Unit, FrogVariant, EntityVariantLayout.Simple, SimpleEntityVariantLayoutBuilder>(
    Registries.FROG_VARIANT,
    CraftFrog.CraftVariant::minecraftHolderToBukkit,
    Unit,
    ::SimpleEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Simple, spawnConditions: SpawnPrioritySelectors) =
        FrogVariant(ClientAsset.ResourceTexture(layout.texture.toIdentifier()), spawnConditions)
    
}