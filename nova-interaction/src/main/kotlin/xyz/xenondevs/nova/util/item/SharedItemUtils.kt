package xyz.xenondevs.nova.util.item

import net.minecraft.world.item.ItemStack as NmsItemStack
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicReference

// TODO: Move shared utilities into a dedicated utility module.

/**
 * Damages the [this][ItemStack] by [amount] and returns the resulting [ItemStack],
 * which may be `null` if the item broke.
 */
fun ItemStack.damage(amount: Int, world: World): ItemStack? {
    val nms = CraftItemStack.unwrap(this)
    val ref = AtomicReference(nms)
    nms.hurtAndBreak(amount, (world as CraftWorld).handle, null) {
        ref.set(NmsItemStack.EMPTY)
    }
    return CraftItemStack.asBukkitMirror(ref.get()).takeUnless { it.isEmpty }
}
