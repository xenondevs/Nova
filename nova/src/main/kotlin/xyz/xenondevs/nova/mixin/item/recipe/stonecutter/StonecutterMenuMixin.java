package xyz.xenondevs.nova.mixin.item.recipe.stonecutter;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Prevents desyncs between client- and server-side suggested stonecutter recipes
 * due to more strict matching (client does not check for nova id) on the server.
 */
@Mixin(StonecutterMenu.class)
abstract class StonecutterMenuMixin {
    
    @Shadow
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput;
    
    @Final
    @Shadow
    private Player player;
    
    @Inject(method = "setupRecipeList", at = @At(value = "RETURN"))
    private void sendRecipesToClient(ItemStack stack, CallbackInfo ci) {
        var thisRef = (StonecutterMenu) (Object) this;
        var recipeManager = MinecraftServer.getServer().getRecipeManager();
        var connection = ((CraftPlayer) player).getHandle().connection;
        
        var packets = List.<Packet<? super ClientGamePacketListener>>of(
            // update the client-side recipes to the ones expected by the server
            new ClientboundUpdateRecipesPacket(
                recipeManager.getSynchronizedItemProperties(),
                recipesForInput
            ),
            // to force the client to recalculate the recipe list, the input needs to be removed and re-added
            new ClientboundContainerSetSlotPacket(
                thisRef.containerId,
                thisRef.incrementStateId(),
                0,
                ItemStack.EMPTY
            ),
            new ClientboundContainerSetSlotPacket(
                thisRef.containerId,
                thisRef.incrementStateId(),
                0,
                stack
            )
        );
        
        connection.send(new ClientboundBundlePacket(packets));
    }
    
}
