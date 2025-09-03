package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponents
import net.minecraft.stats.Stats
import net.minecraft.tags.ItemTags
import net.minecraft.world.level.block.LayeredCauldronBlock
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.CauldronLevelChangeEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsState
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.pos

/**
 * Makes items dyeable.
 */
object Dyeable : ItemBehavior {
    
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.DYEABLE))
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        val event = wrappedEvent.event
        val clickedBlock = event.clickedBlock
        if (
            action == Action.RIGHT_CLICK_BLOCK
            && clickedBlock?.type == Material.WATER_CAULDRON
            && itemStack.unwrap().has(DataComponents.DYED_COLOR)
            && LayeredCauldronBlock.lowerFillLevel(
                clickedBlock.nmsState,
                clickedBlock.world.serverLevel,
                clickedBlock.pos.nmsPos,
                player.serverPlayer,
                CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH
            )
        ) {
            itemStack.unwrap().remove(DataComponents.DYED_COLOR)
            player.serverPlayer.awardStat(Stats.CLEAN_ARMOR)
            wrappedEvent.actionPerformed = true
        }
    }
    
    
    /**
     * Checks whether the given [itemStack] is dyeable, regardless of whether it is a Nova item or not.
     */
    @JvmStatic
    internal fun isDyeable(itemStack: net.minecraft.world.item.ItemStack): Boolean {
        return itemStack.novaItem?.hasBehavior<Dyeable>() ?: itemStack.`is`(ItemTags.DYEABLE)
    }
        
}