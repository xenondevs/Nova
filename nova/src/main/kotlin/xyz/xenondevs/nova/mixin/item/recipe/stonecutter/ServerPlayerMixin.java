package xyz.xenondevs.nova.mixin.item.recipe.stonecutter;

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preemptively removes all client-side stonecutter recipes to prevent incorrect recipes from popping up.
 * Correct recipes are then sent in the StonecutterMenuMixin after the recipe list is calculated.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    
    @Inject(method = "initMenu", at = @At("RETURN"))
    private void removeStonecutterRecipes(AbstractContainerMenu menu, CallbackInfo ci) {
        if (!(menu instanceof StonecutterMenu))
            return;
        
        var recipeManager = MinecraftServer.getServer().getRecipeManager();
        ((ServerPlayer) (Object) this).connection.send(new ClientboundUpdateRecipesPacket(
            recipeManager.getSynchronizedItemProperties(),
            SelectableRecipe.SingleInputSet.empty()
        ));
    }
    
}
