package xyz.xenondevs.nova.mixin.item.tool;

import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xyz.xenondevs.nova.util.item.ToolUtils;

@Mixin(CraftBlock.class)
abstract class CraftBlockMixin {
    
    /**
     * @author NichtStudioCode
     * @reason Nova replaces block breaking logic
     */
    @Overwrite
    public boolean isPreferredTool(ItemStack tool) {
        var block = (CraftBlock) (Object) this;
        return ToolUtils.INSTANCE.isCorrectToolForDrops(block, tool);
    }
    
}
