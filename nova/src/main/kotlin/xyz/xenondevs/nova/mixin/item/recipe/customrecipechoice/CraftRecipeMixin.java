package xyz.xenondevs.nova.mixin.item.recipe.customrecipechoice;

import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.item.recipe.CustomRecipeChoice;

@Mixin(CraftRecipe.class)
interface CraftRecipeMixin {
    
    @Inject(method = "toIngredient", at = @At("HEAD"), cancellable = true)
    private static void toIngredient(RecipeChoice bukkit, boolean requireNotEmpty, CallbackInfoReturnable<Ingredient> cir) {
        if (!(bukkit instanceof CustomRecipeChoice crc))
            return;
        var stacks = crc.getItemStacks$nova().stream()
            .map(is -> CraftItemStack.unwrap(is).copy())
            .toList();
        cir.setReturnValue(Ingredient.ofStacks(stacks));
    }
    
}
