package xyz.xenondevs.nova.mixin.item.enchantment;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.item.NovaItem;
import xyz.xenondevs.nova.world.item.behavior.Enchantable;
import xyz.xenondevs.nova.world.item.enchantment.CustomEnchantmentLogic;

import java.util.Objects;

@Mixin(Enchantment.class)
abstract class EnchantmentMixin {
    
    @ModifyReturnValue(method = "isPrimaryItem", at = @At("RETURN"))
    private boolean isPrimaryItem(
        boolean original,
        @Local(argsOnly = true, name = "item") ItemStack item
    ) {
        var ench = (Enchantment) (Object) this;
        return original || ench.isSupportedItem(item) && nova$isPrimaryItem(item);
    }
    
    @ModifyReturnValue(method = "isSupportedItem", at = @At("RETURN"))
    private boolean isSupportedItem(
        boolean original,
        @Local(argsOnly = true, name = "item") ItemStack item
    ) {
        return original || nova$isSupportedItem(item);
    }
    
    @ModifyReturnValue(method = "canEnchant", at = @At("RETURN"))
    private boolean canEnchant(
        boolean original,
        @Local(argsOnly = true, name = "itemStack") ItemStack itemStack
    ) {
        return original || nova$isSupportedItem(itemStack);
    }
    
    @Unique
    private boolean nova$isPrimaryItem(ItemStack stack) {
        if (!(stack.getItem() instanceof NovaItem novaItem))
            return false;
        
        var ench = (Enchantment) (Object) this;
        var bukkitEnch = nova$minecraftEnchantmentToBukkit(ench);
        var enchantable = novaItem.getBehaviorOrNull(Enchantable.class);
        return enchantable != null && enchantable.getPrimaryEnchantments().contains(bukkitEnch);
    }
    
    @Unique
    private boolean nova$isSupportedItem(ItemStack stack) {
        if (!(stack.getItem() instanceof NovaItem novaItem))
            return false;
        
        var ench = (Enchantment) (Object) this;
        var bukkitEnch = nova$minecraftEnchantmentToBukkit(ench);
        var enchantable = novaItem.getBehaviorOrNull(Enchantable.class);
        return enchantable != null && enchantable.getSupportedEnchantments().contains(bukkitEnch);
    }
    
    @Inject(method = "getMinCost", at = @At("HEAD"), cancellable = true)
    private void getMinCost(int level, CallbackInfoReturnable<Integer> cir) {
        var cel = CustomEnchantmentLogic.customEnchantments.get((Enchantment) (Object) this);
        if (cel != null)
            cir.setReturnValue(cel.getMinCost(level));
    }
    
    @Inject(method = "getMaxCost", at = @At("HEAD"), cancellable = true)
    private void getMaxCost(int level, CallbackInfoReturnable<Integer> cir) {
        var cel = CustomEnchantmentLogic.customEnchantments.get((Enchantment) (Object) this);
        if (cel != null)
            cir.setReturnValue(cel.getMaxCost(level));
    }
    
    @Unique
    private org.bukkit.enchantments.Enchantment nova$minecraftEnchantmentToBukkit(Enchantment enchantment) {
        var id = MinecraftServer.getServer().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getKey(enchantment);
        Objects.requireNonNull(id);
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ENCHANTMENT)
            .get(Key.key(id.getNamespace(), id.getPath()));
    }
    
}
