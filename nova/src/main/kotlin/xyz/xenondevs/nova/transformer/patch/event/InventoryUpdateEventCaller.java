package xyz.xenondevs.nova.transformer.patch.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import xyz.xenondevs.nova.event.PlayerInventoryItemChangeEvent;
import xyz.xenondevs.nova.event.PlayerInventoryUpdateEvent;

public class InventoryUpdateEventCaller {
    
    public static void call(int oldAmount, int newAmount, ItemStack item) {
        if (oldAmount == newAmount || oldAmount <= 0 && newAmount <= 0 || item.is(Items.AIR)) {
            return;
        }
        var invData = PlayerInventoryUpdateEventPatch.itemStackToPlayerInventory.get(item);
        if (invData == null) return;
        var player = invData.player;
        var slot = invData.slot;
        var oldItem = oldAmount <= 0 ? null : CraftItemStack.asCraftMirror(item.copyWithCount(oldAmount));
        var newItem = newAmount <= 0 ? null : CraftItemStack.asCraftMirror(item.copyWithCount(newAmount));
        var updateEvent = new PlayerInventoryUpdateEvent(player, slot, oldItem, newItem);
        Bukkit.getPluginManager().callEvent(updateEvent);
        if ((oldItem != null) ^ (newItem != null)) {
            var changeEvent = new PlayerInventoryItemChangeEvent(player, slot, oldItem, newItem);
            Bukkit.getPluginManager().callEvent(changeEvent);
        }
    }
    
}
