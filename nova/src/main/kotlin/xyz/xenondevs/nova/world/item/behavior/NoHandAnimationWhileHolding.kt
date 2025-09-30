package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import net.kyori.adventure.key.Key
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.util.PlayerMapManager
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.util.serverPlayer
import kotlin.math.ceil

/**
 * Disables the hand animation of pulling out the item while the item is being held,
 * meaning that updating the item while the player is holding it will not play the animation again.
 * 
 * Does currently not work for switching between item types: [MC-301863](https://report.bugs.mojang.com/servicedesk/customer/portal/2/MC-301863).
 */
object NoHandAnimationWhileHolding : ItemBehavior {
    
    private val updateTasks: MutableMap<Player, BukkitTask> = PlayerMapManager.create()
    
    override fun handleEquip(player: Player, itemStack: ItemStack, slot: EquipmentSlot, equipped: Boolean, event: EntityEquipmentChangedEvent) {
        if (!slot.isHand)
            return
        
        // refer to ItemInHandRenderer.java (client)
        val strengthScale = player.serverPlayer.getAttackStrengthScale(1f)
        val pullUpTicks = ceil(1f / (strengthScale * strengthScale * strengthScale).coerceAtMost(0.4f)).toInt()
        val totalAnimationTime = 3L + pullUpTicks // 3 ticks for pull down
        
        // Needs to update entire inventory because:
        // 1. for unequipping, we don't know which slot was unequipped
        // 2. for equipping, we also need to update all similar items in the inventory,
        //    because handleEquip will not be called when switching between them
        updateTasks.remove(player)?.cancel()
        if (equipped) {
            updateTasks[player] = runTaskLater(totalAnimationTime) {
                player.updateInventory()
            }
        } else {
            player.updateInventory()
        }
    }
    
    // this is technically not thread-safe, but shouldn't cause any issues in practice
    // worst case scenario is that the animation doesn't get disabled or gets incorrectly disabled shortly
    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        // needs to be disabled in creative mode, otherwise the item stack will disappear when trying to pick it up
        if (player?.gameMode == GameMode.CREATIVE)
            return client
        
        if (player?.inventory?.itemInMainHand == server || player?.inventory?.itemInOffHand == server) {
            val itemModel = client.getData(DataComponentTypes.ITEM_MODEL)
                ?: return client
            client.setData(DataComponentTypes.ITEM_MODEL, Key.key(itemModel.namespace(), itemModel.value() + "_no_hand_animation"))
        }
        return client
    }
    
}