package xyz.xenondevs.nova.world.entity.variant

import net.kyori.adventure.key.Key
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.animal.wolf.WolfVariant
import net.minecraft.world.entity.variant.SpawnPrioritySelectors
import org.bukkit.craftbukkit.entity.CraftWolf
import org.bukkit.entity.Wolf
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.layout.entity.WolfEntityVariantLayoutBuilder
import xyz.xenondevs.nova.util.toResourceLocation

class WolfVariantBuilder internal constructor(
    id: Key
) : EntityVariantBuilder<Wolf.Variant, Unit, WolfVariant, EntityVariantLayout.Wolf, WolfEntityVariantLayoutBuilder>(
    Registries.WOLF_VARIANT,
    CraftWolf.CraftVariant::minecraftHolderToBukkit,
    Unit,
    ::WolfEntityVariantLayoutBuilder,
    id
) {
    
    override fun build(modelType: Unit, layout: EntityVariantLayout.Wolf, spawnConditions: SpawnPrioritySelectors): WolfVariant {
        return WolfVariant(
            WolfVariant.AssetInfo(
                ClientAsset.ResourceTexture(layout.wild.toResourceLocation()),
                ClientAsset.ResourceTexture(layout.tame.toResourceLocation()),
                ClientAsset.ResourceTexture(layout.angry.toResourceLocation()),
            ),
            spawnConditions,
        )
    }
    
}