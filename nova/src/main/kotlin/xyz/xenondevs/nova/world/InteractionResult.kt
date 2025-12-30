package xyz.xenondevs.nova.world

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.world.item.ItemAction
import xyz.xenondevs.nova.world.player.swingHandEventless
import net.minecraft.world.InteractionResult as NmsInteractionResult

/**
 * The result of an interaction attempt.
 */
sealed interface InteractionResult {
    
    /**
     * No interaction occurred, pass to the next handler.
     */
    data object Pass : InteractionResult
    
    /**
     * An interaction completed successfully, stop processing further handlers.
     */
    data class Success(
        /**
         * Whether the interacting entity's hand should swing.
         * Note that depending on client-side predictions, a hand swing may occur even if this is set to false.
         */
        val swing: Boolean = false,
        /**
         * The action to perform on the item used for the interaction.
         * - Use `null` if the held item was not involved in the interaction.
         * - Use [ItemAction.None] if the held item was involved but no action should be performed.
         */
        val action: ItemAction? = null
    ) : InteractionResult {
        
        /**
         * Whether the held item was involved in the interaction.
         */
        val wasItemInteraction: Boolean
            get() = action != null
        
        /**
         * Performs the actions associated with this result as if it was [entity] that used [hand].
         */
        fun performActions(entity: LivingEntity, hand: EquipmentSlot) {
            if (swing)
                entity.swingHandEventless(hand)
            
            if (action != null) {
                // possibly apply item cooldown
                if (entity is Player) {
                    val handItem = entity.inventory.getItem(hand)
                    val cooldown = handItem.getData(DataComponentTypes.USE_COOLDOWN)
                    if (cooldown != null) {
                        entity.setCooldown(handItem, (cooldown.seconds() * 20).toInt())
                    }
                }
                
                action.apply(entity, hand)
            }
        }
        
    }
    
    /**
     * The interaction failed, stop processing further handlers.
     */
    data object Fail : InteractionResult
    
}

internal fun InteractionResult.toNms(): NmsInteractionResult {
    return when (this) {
        is InteractionResult.Success -> NmsInteractionResult.Success(
            NmsInteractionResult.SwingSource.NONE,
            NmsInteractionResult.ItemContext(wasItemInteraction, null) // TODO: should this expose transformedTo ???
        )
        is InteractionResult.Fail -> NmsInteractionResult.FAIL
        is InteractionResult.Pass -> NmsInteractionResult.PASS
    }
}