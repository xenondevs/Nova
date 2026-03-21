package xyz.xenondevs.nova.mixin.item.data;

import net.kyori.adventure.key.Key;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.NovaDataComponentMap;
import xyz.xenondevs.nova.world.item.legacy.ItemStackLegacyConversion;

@SuppressWarnings({"deprecation"})
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    
    @Redirect(
        method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/component/PatchedDataComponentMap;fromPatch(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/core/component/DataComponentPatch;)Lnet/minecraft/core/component/PatchedDataComponentMap;"
        )
    )
    private static PatchedDataComponentMap fromPatch(DataComponentMap prototype, DataComponentPatch oldPatch) {
        var patch = ItemStackLegacyConversion.convert(prototype, oldPatch);
        var newPrototype = prototype;
        
        var component = patch.get(prototype,DataComponents.CUSTOM_DATA);
        if (component != null) {
            var novaKey = component.getUnsafe()
                .getCompound("nova")
                .flatMap(tag -> tag.getString("id"));
            if (novaKey.isPresent()) {
                var novaKeyString = novaKey.get();
                if (Key.parseable(novaKeyString)) {
                    newPrototype = new NovaDataComponentMap(Key.key(novaKeyString), prototype);
                }
            }
        }
        
        return PatchedDataComponentMap.fromPatch(newPrototype, patch);
    }
}
