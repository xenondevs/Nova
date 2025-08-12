package xyz.xenondevs.nova.mixin.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.registry.NovaRegistries;
import xyz.xenondevs.nova.world.item.NovaDataComponentMap;
import xyz.xenondevs.nova.world.item.legacy.ItemStackLegacyConversion;

@SuppressWarnings({"OptionalAssignedToNull", "deprecation"})
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    
    @Redirect(
        method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/component/PatchedDataComponentMap;fromPatch(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/core/component/DataComponentPatch;)Lnet/minecraft/core/component/PatchedDataComponentMap;"
        )
    )
    private static PatchedDataComponentMap fromPatch(DataComponentMap patchedDataComponentMap, DataComponentPatch prototype) {
        var patch = ItemStackLegacyConversion.convert(prototype);
        var newMap = patchedDataComponentMap;
        
        var component = patch.get(DataComponents.CUSTOM_DATA);
        if (component != null) {
            
            var novaItem = component
                .map(CustomData::getUnsafe)
                .flatMap(tag -> tag.getCompound("nova"))
                .flatMap(tag -> tag.getString("id"))
                .map(id -> NovaRegistries.ITEM.getValue(ResourceLocation.parse(id)));
            if (novaItem.isPresent()) {
                newMap = new NovaDataComponentMap(novaItem.get());
            }
        }
        
        return PatchedDataComponentMap.fromPatch(newMap, patch);
    }
}
