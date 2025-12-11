package xyz.xenondevs.nova.mixin.item.enchantment;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.item.behavior.Enchantable;
import xyz.xenondevs.nova.world.item.enchantment.CustomEnchantmentLogic;

import java.util.Objects;

@Mixin(Enchantment.class)
abstract class EnchantmentMixin {
    
    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void isPrimaryItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var ench = (Enchantment) (Object) this;
        var cel = CustomEnchantmentLogic.customEnchantments.get(ench);
        if (cel != null && cel.isPrimaryItem(stack.asBukkitMirror())) {
            cir.setReturnValue(true);
            return;
        }
        
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem == null)
            return;
        
        var bukkitEnch = nova$minecraftEnchantmentToBukkit(ench);
        var enchantable = novaItem.getBehaviorOrNull(Enchantable.class);
        cir.setReturnValue(enchantable != null && enchantable.getPrimaryEnchantments().contains(bukkitEnch));
    }
    
    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void isSupportedItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var supported = nova$isSupportedItem(stack);
        if (supported != null)
            cir.setReturnValue(supported);
    }
    
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void canEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var supported = nova$isSupportedItem(stack);
        if (supported != null)
            cir.setReturnValue(supported);
    }
    
    @Unique
    private Boolean nova$isSupportedItem(ItemStack stack) {
        var ench = (Enchantment) (Object) this;
        var cel = CustomEnchantmentLogic.customEnchantments.get(ench);
        if (cel != null && cel.isSupportedItem(stack.asBukkitMirror()))
            return true;
        
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem == null)
            return null;
        
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
    
    @SuppressWarnings("OverwriteAuthorRequired")
    @Overwrite
    public static boolean areCompatible(Holder<Enchantment> first, Holder<Enchantment> second) {
        if (first == second)
            return false;
        
        var celFirst = CustomEnchantmentLogic.customEnchantments.get(first.value());
        var celSecond = CustomEnchantmentLogic.customEnchantments.get(second.value());
        
        boolean firstCompatibleWithSecond = celFirst != null 
            ? celFirst.compatibleWith(second) 
            : !first.value().exclusiveSet().contains(second);
        boolean secondCompatibleWithFirst = celSecond != null
            ? celSecond.compatibleWith(first)
            : !second.value().exclusiveSet().contains(first);
        
        return firstCompatibleWithSecond && secondCompatibleWithFirst;
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
