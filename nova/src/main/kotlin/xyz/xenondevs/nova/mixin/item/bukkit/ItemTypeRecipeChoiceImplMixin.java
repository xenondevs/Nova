package xyz.xenondevs.nova.mixin.item.bukkit;

import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {"org.bukkit.inventory.ItemTypeRecipeChoiceImpl"})
abstract class ItemTypeRecipeChoiceImplMixin {
    
    @SuppressWarnings("unchecked")
    @Redirect(
        method = "getChoices",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/Registry;getOrThrow(Lnet/kyori/adventure/key/Key;)Lorg/bukkit/Keyed;"
        )
    )
    private <T extends Keyed> T retrieveMaterialThroughItemType(Registry<T> instance, Key key) {
        var result = instance.get(key);
        return result != null ? result : (T) Material.SHULKER_SHELL;
    }
    
}
